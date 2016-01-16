# titan-perf-tester

This utility was created to load some test data into Titan 1.0.0 and provides a utility class for running and timing gremlin traversals.

## Setup VM

This project utilizes Vagrant so make sure that is setup.  Once Vagrant is installed, check out this project and from the root of this project bring up vagrant and login

```
vagrant up
vagrant ssh
```

From the home folder in the VM, download and unzip Titan 1.0.0 from https://github.com/thinkaurelius/titan/wiki/Downloads

Use maven to build the jar file and once created, drop it in the titan-1.0.0-hadoop1/ext folder.

View the README file in the data folder for the instructions on where to access the game data.  Download the zip file and unzip the files to the data folder.

Finally, move into the titan folder and run

```
./bin/titan.sh start
./bin/gremlin.sh
```

## Loading Data

From the gremlin shell:

```
graph = TitanFactory.open('conf/titan-cassandra-es.properties')
import us.wellaware.*
TitanPerf.load(graph)
```

## Running test queries

From the gremlin shell and after test data is loaded:

```
//Date filter used in various queries below
dateFilter = lt(20051000)

//Get a list of teams the are in the Big 10 conference
test1 = {g -> g.traversal().V().has('conference', 'Big Ten Conference').toList()}

//Get a list of teams in the Big 10 conference and list out their records
test2 = {g -> g.traversal().V().has('conference', 'Big Ten Conference').as('team', 'wins', 'losses').select('team', 'wins', 'losses').by('name').by(__.outE().count()).by(__.inE().count()).toList()}

//Inefficient way of sorting above traversal by games won
test3 = {g -> g.traversal().V().order().by(__.outE().count(), decr).has('conference', 'Big Ten Conference').as('team', 'wins', 'losses').select('team', 'wins', 'losses').by('name').by(__.outE().count()).by(__.inE().count()).toList()}

//Efficient way of sorting above traversal by games won
test4 = {g -> g.traversal().V().has('conference', 'Big Ten Conference').order().by(__.outE().count(), decr).as('team', 'wins', 'losses').select('team', 'wins', 'losses').by('name').by(__.outE().count()).by(__.inE().count()).toList()}

//Get Big 10 records with the dateFilter applied
test5 = {g -> g.traversal().V().has('conference', 'Big Ten Conference').as('team', 'wins', 'losses').select('team', 'wins', 'losses').by('name').by(__.outE().has('date', dateFilter).count()).by(__.inE().has('date', dateFilter).count()).toList()}

//Run the tests and return the stats for the query
stats1 = PerfTestRunner.test(graph, 10, test1)
stats2 = PerfTestRunner.test(graph, 10, test2)
stats3 = PerfTestRunner.test(graph, 10, test3)
stats4 = PerfTestRunner.test(graph, 10, test4)
stats5 = PerfTestRunner.test(graph, 10, test5)
```
