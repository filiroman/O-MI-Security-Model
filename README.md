# O-MI-Security-Model
Complementary authorization and access control modules for O-MI

Dependencies
------------
- Java-1.7
- O-MI Node 0.2.1

Installation
--------------
- Install [Nginx](http://nginx.org/en/) and configure reverse proxy
- Install [O-MI Node](https://github.com/AaltoAsia/O-MI)
- Install [Apache Maven](https://maven.apache.org/)
- Download [latest zip](https://github.com/filiroman/O-MI-Security-Model/archive/master.zip) and extract it
- Execute `install.sh` inside the project's folder
- Place your Facebook App credentials inside `src/main/java/com/aaltoasia/omi/accontrol/FacebookAuth.java`
- Start `O-MI Node`
- Execute `run.sh` to run the module

Now use [http://localhost:8088/O-MI](http://localhost:8088/O-MI) for Login and Register new users and [http://localhost:8088/AC/webclient](http://localhost:8088/AC/webclient) for Access Control.
