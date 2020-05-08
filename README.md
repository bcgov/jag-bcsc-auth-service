# jag-bcsc-auth-service
BCSC Auth service (eCRC microservice) by Ministry of Justice

Technical Overview
---------------------
| Layer   | Technology |
| ------- | ------------ |
| Service | Java, SpringFramework |
| Application Server | Spring Boot / Tomcat |
| Runtime | Pathfinder OpenShift |

## Installation:
### Local Environment:

### OpenShift:
The latest OpenShift deployment images are available under pqyiwk-tools project. 

To manually deploy the latest image to a target environment:
- Create the deployment configuration if not yet exists. 
- Run oc command. Following is an example.
```
oc rollout latest dc/bcsc-auth-service -n pqyiwk-dev
```

## Files in this repository
```
project
+-- src
|   +-- bcsc-auth
```

DevOps Process
-------------

### Jenkins
Jenkins pipeline is triggered by merge request in the repository.

### DEV builds
Dev builds are triggered by merge request in the repository.

## Promotion to TEST
Login to the OpenShift Web Console and navigate to the Tools project for the system.  Go to Builds->Pipelines.  Click  "Yes" on "Deploy to Test Approval" prompt.

## Promotion to PROD
Login to the OpenShift Web Console and navigate to the Tools project for the system.  Go to Builds->Pipelines.  Click  "Yes" on "Deploy to Prod Approval" prompt.

### Environments

| Environment | URL                                    | VPN Required? |
| ----------- | -------------------------------------- | ------------- |
| DEV         | https://bcsc-auth-service-pqyiwk-dev.pathfinder.gov.bc.ca  | No           |
| TEST        | https://bcsc-auth-service-pqyiwk-test.pathfinder.bcgov | Yes           |
| PROD        | https://bcsc-auth-service-pqyiwk-prod.pathfinder.bcgov      | Yes            |

## Access Control - IP Whitelisting
To allow client application to access this BCSC auth service, the IP address of the client application must be added to the route configuration. Use white space to separate multiple IP addresses. Here is an example:
```
...
annotations:
	haproxy.router.openshift.io/ip_whitelist: 142.34.208.209 142.29.196.27
...
```