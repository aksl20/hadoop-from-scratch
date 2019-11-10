FROM jupyter/minimal-notebook

USER root

RUN apt-get -y update && \
    apt-get install --no-install-recommends -y \
      curl \
      openjdk-11-jre-headless \
      ca-certificates-java \
      ssh \
      dnsutils \
      iputils-ping \
      git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

USER $NB_UID
WORKDIR /home/jovyan/work

# Install java kernel
RUN git clone https://github.com/SpencerPark/IJava.git && \
    cd IJava/ && \
    chmod u+x gradlew && ./gradlew installKernel
ADD kernel.json /home/jovyan/.local/share/jupyter/kernels/java/kernel.json

# download project
RUN git clone https://github.com/aksl20/hadoop-from-scratch.git

# ssh config
RUN mkdir /home/jovyan/.ssh
COPY ssh/id_rsa /home/jovyan/.ssh/
COPY ssh/id_rsa.pub /home/jovyan/.ssh/
COPY ssh/public-keys /home/jovyan/.ssh/authorized_keys
COPY ssh/ssh-config /home/jovyan/.ssh/config

USER root
RUN chown -R jovyan:users /home/jovyan/.ssh

USER $NB_UID
