FROM openjdk:17.0.1-jdk-slim

RUN apt-get update && \
    apt-get install -y wget lsb-release gnupg

RUN echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -

RUN apt-get update && \
    apt-get install -y postgresql-client

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

COPY ./build/libs/database_dump_machine-1.0-SNAPSHOT.jar ./app.jar

CMD ["java", "-jar", "./app.jar"]
