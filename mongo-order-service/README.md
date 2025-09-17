# Run the service

Use java 17 or higher
maven 3.8.x or higher

mvn clean package

# Insert Document

curl -X POST "http://localhost:8080/entity?name=hello"
