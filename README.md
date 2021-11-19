# Netflix conductor POC
This repository holds all my POC of Netflix conductor.

To run this POC you will need:
* Netflix conductor installed and running (check https://netflix.github.io/conductor/server/#3-use-the-pre-configured-docker-image)

Before running the POC application, Tasks and Workflow need to be configured. To achieve this, just run the following:
* Tasks: `WorkflowDefinitionTest.registerTasks`
* Workflow: `WorkflowDefinitionTest.registerWorkflow`

After that, you can start the spring-boot application by typing on command line:
```bash
$ mvn spring-boot:run
```

On another terminal, submit some orders to be processed:
```bash
for i in {1..10}; do 
  curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X POST http://localhost:8081/order -d '{ "orderId": "1" }'; 
done;
```

Now navigate to `http://localhost:5000` to check your workflow UI and the application logs.

