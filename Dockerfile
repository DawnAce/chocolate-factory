FROM gradle:jdk17 as build
WORKDIR /app
COPY . .
RUN rm -rf choc-ui
RUN ./gradlew build --no-daemon

FROM openjdk:17-jdk
WORKDIR /app
COPY --from=build /app/build/libs/chocolate-factory-*.jar app.jar
RUN ls -la
ENV OPENAI_HOST "https://api.openai.com/"
ENV OPENAI_API_KEY ""
CMD ["java", "-jar", "app.jar"]
EXPOSE 18080