# Netflix conductor POC
This repository holds all my POC of Netflix conductor.

To run this POC you will need:
* Netflix conductor installed and running (check https://netflix.github.io/conductor/server/#3-use-the-pre-configured-docker-image)
* Verify conductor can reach host endpoints, if not check [this discussion](https://github.com/Netflix/conductor/discussions/2591) on how to enable it.

Before running the POC application, Tasks and Workflow need to be configured. To achieve this, just run the following:
* for the Worker POC:
    * Tasks: `WorkflowDefinitionTest.registerTasks`
    * Workflow: `WorkflowDefinitionTest.registerWorkersWorkflow`
* for the Event POC:
    * Workflows: `WorkflowDefinitionTest.registerEventWorkflow`
    * EventHandlers: `WorkflowDefinitionTest.registerEventHandlers`

After that, you can start the spring-boot application by typing on command line:
```bash
$ mvn spring-boot:run
```

On another terminal, submit some orders to be processed:
```bash
for i in {1..10}; do 
  # to submit to the workflow that uses workers
  curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X POST http://localhost:8080/order -d '{ "orderId": "1", "workflowDefinition": "Order-Worker-POC", "version": 1 }'
  # to submit to the workflow that uses http & event handlers
  curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X POST http://localhost:8080/order -d '{ "orderId": "1", "workflowDefinition": "Order-Event-POC", "version": 1 }' 
done;
```

Now navigate to `http://localhost:5000` to check your workflow UI and the application logs.

**NOTE:** The event workflow is unfinished due to some issues I didn't resolve on how to add output data to the WAIT task from the awaken event handler.


