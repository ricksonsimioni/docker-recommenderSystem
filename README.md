Of course. I've adjusted your README to be clearer and to use the more advanced, automated setup we designed.

This new version simplifies the process for the end-user significantly. Instead of cloning the code themselves and creating complex folder structures, they now only have to create one empty folder and run a single command. The container handles the rest.

-----

## Revised `README.md`

# Generic Recommender System Project

This project is a generic recommender system developed using Epsilon and Java. It runs inside a custom Docker container that provides a complete, web-accessible Eclipse IDE with all dependencies pre-installed.

-----

## Quick Start

This environment is designed for maximum simplicity. On the first launch, the container will automatically clone the project source code for you.

### **Prerequisites**

  * [Docker Desktop](https://www.docker.com/products/docker-desktop/) must be installed and running.

### **1. Create Your Project Folder**

Create a single, empty folder on your computer. This folder will store your project files and the container's persistent settings. Then, navigate into it.

```bash
mkdir my-recommender-project && cd my-recommender-project
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

-----

## How to Stop and Start

  * To **stop** the container: `docker stop recommender-ide`
  * To **start** it again: `docker start recommender-ide`
