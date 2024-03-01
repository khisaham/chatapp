FROM maven:3.8.4-jdk-11 AS maven_build
LABEL authors="khisa"

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copy the entire source code to the container
COPY src ./src
RUN mvn package -DskipTests

FROM tomcat:9-jdk11

COPY --from=maven_build /app/target/*.war /usr/local/tomcat/webapps/

RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Use a base image with Nginx for serving the Angular app
FROM nginx:latest

# Copy the built Angular app from the angular_build stage to the Nginx html directory
COPY --from=angular_build /app/dist/angular-app /usr/share/nginx/html

# Expose ports
EXPOSE 8080 80

# Start both Tomcat and Nginx
CMD service nginx start && catalina.sh run
