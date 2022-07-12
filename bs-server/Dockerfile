FROM java:8

# image layer
WORKDIR /app
ADD https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar ./

ENTRYPOINT ["java","-jar","/app/leshan-bsserver-demo.jar","-wp","8888","-lp","5783","-slp","5784","-cfg","/app/config.json"]