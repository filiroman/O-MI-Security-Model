# O-MI-Security-Model
Complementary authorization and access control modules for O-MI

Dependencies
------------
- java-1.7
- O-MI Node 0.2.1

Java libraries (Maven)
--------------
- gson-2.5
- scribe-1.3.7
- sqlite-jdbc-3.8.11.2
- jetty-server 9.2.7.v20150116
- jetty-servlet 9.2.7.v20150116

Repository also includes IntelliJ IDEA project

Usage
------------
1. Install [O-MI Node](https://github.com/AaltoAsia/O-MI).
2. Download [latest zip](https://github.com/filiroman/O-MI-Security-Model/archive/master.zip) and extract it.
3. Start `O-MI Node`
4. Start Access Control Server by executing Main method of `com.aaltoasia.omi.accontrol.http.HttpServer` class.

Now use [http://localhost:8088/O-MI](http://localhost:8088/O-MI) for Login and Register new users and [http://localhost:8088/AC/webclient](http://localhost:8088/AC/webclient) for Access Control.
