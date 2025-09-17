
echo "Create roles for mongo CDC..."


docker exec -i mongo mongosh << EOF
  rsconf = {
    _id: "rs0",
    members: [{ _id: 0, host: "mongo:27017", priority: 1.0 }],
  };
  rs.initiate(rsconf);
  rs.status();
EOF

docker exec -i mongo mongosh << EOF
use admin
db.createRole({
   role: "confluentCDCRole",
   privileges: [
      { resource: { cluster: true }, actions: ["find", "changeStream"] },
      { resource: { db: "outbox", collection: "loans" }, actions: [ "find", "changeStream" ] }
   ],
   roles: []
});

db.createUser({
  user: "data-platform-cdc",
  pwd: "password",
  roles: [
    { role: "read", db: "admin" },
    { role: "clusterMonitor", db: "admin" },
    { role: "read", db: "config" },
    { role: "read", db: "outbox" },
    { role: "confluentCDCRole", db: "admin"}
  ]
});

EOF
