# Kitchen Sink

This project serves as a testing, diagnostic and demonstration application for Vaadin, Control Center and the Acceleration Kits.

## Running the application

To run the application execute the `spring-boot:run` goal using Maven, optionally specifying a profile for Control Center or a Kit:

**Control Center**
```
./mvnw spring-boot:run -Pcontrol-center
```

**Kubernetes Kit**
```
./mvnw spring-boot:run -Pkubernetes-kit
```

**Observability Kit**
```
./mvnw spring-boot:run -Pobservability-kit
```

## Deploying to Production

The project is a standard Maven project. To create a production build, call 

```
./mvnw clean package -Pproduction
```

If you have Maven globally installed, you can replace `./mvnw` with `mvn`.

This will build a JAR file with all the dependencies and front-end resources,ready to be run. The file can be found in the `target` folder after the build completes.
You then launch the application using 
```
java -jar target/kitchen-sink-1.0-SNAPSHOT.jar
```

## Deploying using Docker

To build a Docker image for the application, you can execute the `spring-boot:build-image` goal using Maven:

```
./mvnw clean spring-boot:build-image -Pproduction -Dspring-boot.build-image.imagePlatform=linux/amd64 -Dspring-boot.build-image.imageName=kitchen-sink:1.0-SNAPSHOT
```

Again, you can optionally specify a profile for Control Center or a Kit.
