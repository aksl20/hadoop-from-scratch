FROM debian

RUN apt-get -y update && \
    apt-get install --no-install-recommends -y \
      curl \
      openjdk-11-jre-headless \
      ca-certificates-java \
      ssh \
      git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir /var/run/sshd

EXPOSE 22

COPY ssh/id_rsa /root/.ssh/
COPY ssh/id_rsa.pub /root/.ssh/
COPY ssh/public-keys /root/.ssh/authorized_keys

CMD ["/usr/sbin/sshd", "-D"]