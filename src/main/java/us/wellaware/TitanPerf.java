package us.wellaware;

import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nakuljeirath on 1/1/16.
 */
public class TitanPerf {
    private static final String SQL_OUTPUT = "/vagrant/load.sql";

    public static void load(final TitanGraph graph) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(SQL_OUTPUT));
        setupSchema(graph, bw);

        TitanTransaction tx = graph.newTransaction();
        Map<Integer, Vertex> teamIdToVertexMap = setupTeams(tx, bw);
        setupBeatEdges(tx, teamIdToVertexMap, bw);
        tx.commit();
        bw.flush();
        bw.close();
    }

    public static void setupSchema(TitanGraph graph, BufferedWriter sqlWriter) throws IOException {
        TitanManagement mgmt = graph.openManagement();
        PropertyKey confKey = mgmt.makePropertyKey("conference").dataType(String.class).make();
        mgmt.makePropertyKey("name").dataType(String.class).make();

        EdgeLabel beatEdge = mgmt.makeEdgeLabel("beat").make();
        PropertyKey dateKey = mgmt.makePropertyKey("date").dataType(Long.class).make();
        PropertyKey winKey = mgmt.makePropertyKey("winScore").dataType(Integer.class).make();
        mgmt.buildIndex("byConference", Vertex.class).addKey(confKey).buildCompositeIndex();
//        mgmt.buildEdgeIndex(beatEdge, "beatByDate", Direction.BOTH, dateKey, winKey);
        mgmt.commit();


        sqlWriter.write("create table teams (\n" +
                "\tteam_id integer primary key,\n" +
                "\tconference varchar(100),\n" +
                "\tname varchar(100)\n" +
                ");\n");
        sqlWriter.write("create table beat (\n" +
                "\twinner integer references teams(team_id),\n" +
                "\tloser integer references teams(team_id),\n" +
                "\twin_score integer,\n" +
                "\tlose_score integer\n" +
                ");\n");
    }

    private static void setupBeatEdges(TitanTransaction tx, Map<Integer, Vertex> teamIdToVertexMap, BufferedWriter sqlWriter) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("/vagrant/data/game.csv"));

        Map<String, Game> gameMap = new HashMap<String, Game>();

        br.readLine();  //Throw away header
        String line = null;

        while ((line = br.readLine()) != null) {
            Game game = new Game();
            String[] parts = line.split(",");
            String gameId = parts[0];
            String[] dateParts = parts[1].split("/");
            game.awayTeamId = Integer.parseInt(parts[2]);
            game.homeTeamId = Integer.parseInt(parts[3]);
            game.date = Long.parseLong(dateParts[2] + dateParts[0] + dateParts[1]);

            gameMap.put(gameId, game);
        }

        br.close();

        BufferedReader br2 = new BufferedReader(new FileReader("/vagrant/data/team-game-statistics.csv"));
        br2.readLine();

        while ((line = br2.readLine()) != null) {
            String[] parts = line.split(",");
            Integer teamId = Integer.parseInt(parts[0]);
            String gameId = parts[1];
            Integer points = Integer.parseInt(parts[35]);

            Game game = gameMap.get(gameId);
            if (game.homeTeamId == teamId) {
                game.homeTeamScore = points;
            } else {
                game.awayTeamScore = points;
            }
        }

        br2.close();

        for (Game game : gameMap.values()) {
            if (game.homeTeamScore > game.awayTeamScore) {
                teamIdToVertexMap.get(game.homeTeamId).addEdge("beat", teamIdToVertexMap.get(game.awayTeamId),
                        "winScore", game.homeTeamScore, "loseScore", game.awayTeamScore, "date", game.date);
                sqlWriter.write(String.format("insert into beat values (%d, %d, %d, %d);\n", game.homeTeamId, game.awayTeamId,
                        game.homeTeamScore, game.awayTeamScore));

            } else {
                teamIdToVertexMap.get(game.awayTeamId).addEdge("beat", teamIdToVertexMap.get(game.homeTeamId),
                        "winScore", game.awayTeamScore, "loseScore", game.homeTeamScore, "date", game.date);
                sqlWriter.write(String.format("insert into beat values (%d, %d, %d, %d);\n", game.awayTeamId, game.homeTeamId,
                        game.awayTeamScore, game.homeTeamScore));
            }
        }
    }

    private static Map<Integer, Vertex> setupTeams(TitanTransaction tx, BufferedWriter sqlWriter) throws IOException {
        Map<Integer, String > confIdToNameMap = loadConferences();

        BufferedReader br = new BufferedReader(new FileReader("/vagrant/data/team.csv"));

        br.readLine();  //Throw away header
        String line = null;

        Map<Integer, Vertex> teamIdToVertexMap = new HashMap<Integer, Vertex>();

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            Integer teamId = Integer.parseInt(parts[0]);
            String teamName = parts[1].replace("\"", "");
            Integer confId = Integer.parseInt(parts[2]);
            String conference = confIdToNameMap.get(confId);

            Vertex v = tx.addVertex(T.label, "team", "name", teamName, "conference", conference);
            sqlWriter.write(String.format("insert into teams values (%d, '%s', '%s');\n", teamId, conference, teamName.replace("'", "")));
            teamIdToVertexMap.put(teamId, v);
        }

        br.close();

        return teamIdToVertexMap;
    }

    private static Map<Integer, String> loadConferences() throws IOException {
        File confFile = new File("/vagrant/data/conference.csv");
        BufferedReader br = new BufferedReader(new FileReader(confFile));

        br.readLine();  //Throw away the header line
        String line = null;

        Map<Integer, String> confIdToNameMap = new HashMap<Integer, String>();

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            Integer confId = Integer.parseInt(parts[0]);
            String confName = parts[1].replace("\"", "");
            confIdToNameMap.put(confId, confName);

        }

        br.close();

        return confIdToNameMap;
    }

    private static class Game {
        public long date;
        public int homeTeamId;
        public int homeTeamScore;
        public int awayTeamId;
        public int awayTeamScore;
    }
}
