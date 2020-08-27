## Building package
```bash
sbt universal:stage
```
You'll get runnable application in `target/universal/stage` with scripts in `target/universal/stage/bin`.

To start app you'll have to create postgresql database
```sql
create user cluster with superuser CREATEDB password 'cluster';
create database cluster owner cluster;
```

You'll also have to create DB schema manually using `src/main/resources/schema.sql`

To clean journal and snapshots use
```sql
delete from journal where true;
delete from snapshot where true;
```

Start all nodes in nodes/nodeN directories

You can get cluster state using
```
curl localhost:8552/cluster/members/ | jq .
```

You can get sharding status on node2 using
```
curl localhost:8552/cluster/shards/ChatReader | jq .
curl localhost:8552/cluster/shards/ChatWriter | jq .
```

Stop node3 during slow message sending from node1 to see sharding rebalancing.
