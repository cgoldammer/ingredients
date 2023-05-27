# Rough setup script
# Not fully tested

sudo yum update -y
sudo yum install tmux -y
sudo yum install docker -y

// https://www.cyberciti.biz/faq/how-to-install-docker-on-amazon-linux-2/
sudo usermod -a -G docker ec2-user
id ec2-user
newgrp docker
sudo yum install python3-pip
pip3 install --user docker-compose

sudo systemctl enable docker.service
sudo systemctl start docker.service


# Set up AWS: TODO - need to automate
# aws configure
# Install cloudwatch agent
sudo yum install amazon-cloudwatch-agent  --disablerepo=docker-ce-stable
# Set up config
sudo amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c ssm:AmazonCloudWatch-linux -s

# Set up swap
swapon -s
sudo fallocate -l 5G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile


