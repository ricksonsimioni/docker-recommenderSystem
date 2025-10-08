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
cd docker-recommenderSystem
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
  -v "$(pwd)":/config/workspace/RecommenderSystem \
  --shm-size="2gb" \
  ricksonsimioni/genericrecommendersystem-focusgroup:latest
```

**On Windows (PowerShell):**
```bash
docker run -d \
  --name=recommender-ide \
  -p 3000:3000 \
  -e PUID=1000 \
  -e PGID=1000 \
  -v "${PWD}":/config/workspace/RecommenderSystem \
  --shm-size="2gb" \
  ricksonsimioni/genericrecommendersystem-focusgroup:latest
```

### **4. Access the IDE**
Open your web browser and go to **http://localhost:3000**. The Eclipse IDE will launch automatically with the project ready to go.

---
