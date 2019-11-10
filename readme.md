# Mapreduce from scratch

The goal of this project is to illustrate the concept of mapreduce with a little bit of java code. The source code is
stored in the src folder. It contains a package 'mapreduce' with 5 classes (Master, Slave, Deploy, Clean, Utils). You 
can find the 'rapport-tp-mapreduce.ipynb' notebook at the project's root. This notebook contains explanation about the 
project and many tests (At the moment, the notebook is in french). You will be able to read the notebook by launching 
the docker platform with the following guidelines.

### Prerequisites

You need to install the following dependencies on your environment system:

- [Docker](https://www.docker.com)
- [Docker-compose](https://docs.docker.com/compose/)

### Launch the platform

```sh
$ git clone <repo_url>
$ cd hadoop-from-scratch/docker
$ docker-compose up -d
```

Before accessing to the notebook, you need to copy-past the token of the jupyter notebook. You can find this token by 
running the following commands.

```sh
$ docker exec -it master bash
$ jupyter notebook list
```

You can copy the token for using it when you will access to the platform with this link [notebook](http://localhost:1000)

### Connect to the platform

- go to the url http://localhost:10000 to open a jupyterlab session