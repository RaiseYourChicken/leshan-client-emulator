FROM java:8

# image layer
WORKDIR /app
ADD https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-server-demo.jar ./

ENTRYPOINT ["java","-jar","/app/leshan-server-demo.jar","-wp","8080"]