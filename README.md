# Generic Recommender System Project

This project is a generic recommender system developed using Epsilon and Java. It is designed to run inside a custom Docker container that provides a complete Eclipse-based development environment.

---
## ## Quick Start

### **Prerequisites**
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) must be installed and running.
* [Git](https://git-scm.com/) must be installed.

### **1. Clone the Repository**
First, clone this project's source code to your local machine.
```bash
git clone [https://github.com/ricksonsimioni/docker-recommenderSystem.git](https://github.com/ricksonsimioni/docker-recommenderSystem.git)
```

### **2. Navigate into the Directory**
```bash
cd docker-recommenderSystem/genericRecommenderSystem-FocusGroup/
```

### **3. Run the Development Environment**
Now, run the following command. It will automatically download the correct Docker image from Docker Hub and link it to the source code you just cloned.

**On Mac or Linux:**
```bash
docker run -d \
  --name=recommender-ide \
  -p 3000:3000 \
  -e PUID=$(id -u) \
  -e PGID=$(id -g) \
  -v genericRecommenderSystem-FocusGroup:/config \
  --shm-size="2gb" \
  ricksonsimioni/genericrs-focusgroup:1.0
```

**On Windows (PowerShell):**
```bash
docker run -d \
  --name=recommender-ide \
  -p 3000:3000 \
  -e PUID=1000 \
  -e PGID=1000 \
  -v genericRecommenderSystem-FocusGroup:/config \
  --shm-size="2gb" \
  ricksonsimioni/genericrs-focusgroup:1.0
```

### **4. Access the IDE**
Open your web browser and go to **http://localhost:3000**. Right-click in the Desktop, select ```Create Launcher```.
Name it as ```eclipse``` in the field ```Name```, and lastly, in the field ```Command``` add the following command:
```
/opt/eclipse/eclipse -data /config/workspace/recommender-system
```
Click in ***Create***


---
