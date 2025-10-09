# Generic Recommender System Project

This project is a generic recommender system developed using Epsilon and Java. It runs inside a custom Docker container that provides a complete, web-accessible Eclipse IDE with all dependencies pre-installed.

-----

## Quick Start

This environment is designed for maximum simplicity. On the first launch, the container will automatically clone the project source code for you.

### **Prerequisites**

  * [Docker](https://docs.docker.com/get-started/get-docker/) or [Docker Desktop](https://www.docker.com/products/docker-desktop/) must be installed and running.

### **1. Create Your Project Folder**

Create a single, empty folder on your computer. This folder will store your project files and the container's persistent settings. Then, navigate into it.

```bash
mkdir ~/my-recommender-project
cd ~/my-recommender-project
```

### **2. Run the Development Environment**

Run the command for your operating system from inside the `my-recommender-project` folder.

**On Mac or Linux:**

```bash
docker run -d \
  --name=recommender-ide \
  -p 3000:3000 \
  -e PUID=$(id -u) \
  -e PGID=$(id -g) \
  -v "$(pwd)":/data \
  --shm-size="2gb" \
  ricksonsimioni/genericrs-focusgroup:latest
```

**On Windows (PowerShell):**

```bash
docker run -d \
  --name=recommender-ide \
  -p 3000:3000 \
  -e PUID=1000 \
  -e PGID=1000 \
  -v "${PWD}":/data \
  --shm-size="2gb" \
  ricksonsimioni/genericrs-focusgroup:latest
```

The first time you run this, Docker will download the image from Docker Hub and then automatically clone the project source code into your folder.

### **3. Access the IDE**

Open your web browser and go to:
**`http://localhost:3000`**

To run the Eclipse IDE, Right-click on the Desktop, select ```Create Launcher```.
Name it as ```eclipse``` in the field ```Name```, and lastly, in the field ```Command``` add the following command:
```
/opt/eclipse/eclipse -data /config/workspace/recommender-system
```

Click in ***Create*** and execute the launcher. The project will be located in the `/data/project` directory inside the container's file explorer.

In case ***LibRec*** is not installed, open the terminal and execute the following command:

```
mvn install:install-file \
    -Dfile=/data/project/genericRecommenderSystem-FocusGroup/librec-core-3.0.0.jar \
    -DgroupId=net.librec \
    -DartifactId=librec-core \
    -Dversion=3.0.0 \
    -Dpackaging=jar
```

In case Eclipse points out an error similar to ```_Downloading external resources is disabled_```, follow these steps:
Go to the top bar: Window -> Preference -> Maven -> tick the option ***Download Artifact Javadoc***.

-----

## How to Stop and Start

  * To **stop** the container: `docker stop recommender-ide`
  * To **start** it again: `docker start recommender-ide`
