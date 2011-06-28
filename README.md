LiveTribe SLP/OSGi Module
=====
<em>A Service Location Protocol implementation.</em>

The LiveTribe :: SLP module is an implementation, written in Java and deployable in any standard OSGi framework, of SLP, the [Service Location Protocol](http://en.wikipedia.org/wiki/Service_Location_Protocol).

The Service Location Protocol provides a scalable framework for the discovery and selection of network services.

Traditionally, users have had to find services by knowing the name of the host (or its network address). SLP eliminates the need for the user to know the name of the host that provides a service; rather, the user provides the desired type of service and optional attributes that further describe the service, and SLP resolves the network address on behalf of the user.

What can I use SLP for ?

SLP is useful in all those cases where you need to contact a remote service, but you don't know where it is located, or you don't know its JNDI name.

**Example:** You want to contact an RMI server, but you don't know under which JNDI name the stub has been registered.

**Example:** You want to contact a JMXConnectorServer, but you don't know its JMXServiceURL.

**Example:** You want to know how many JVMs there are in the network, but you don't know the exact number or host location.

**Example:** You want to automatically, with no additional programming, advertise a service URL when your OSGi service is registered and automatically removed when the OSGi service is unregistered.

**Example:** You want to register OSGi service trackers to keep track of service URLs being registered on the network.

**Example:** You want to leverage OSGi's Configuration Admin Service to configure SLP Service Agents and User Agents.

### More Resources ###

*  Discuss LiveTribe at [http://www.livetribe.org/Mailing+Lists](http://www.livetribe.org/Mailing+Lists)
*  Learn even more at [http://www.livetribe.org/LiveTribe-SLP](http://www.livetribe.org/LiveTribe-SLP)
*  Check out code at [https://github.com/livetribe/livetribe-slp](https://github.com/livetribe/livetribe-slp)

#### A CodeHaus project ###
![Alt text](https://github.com/livetribe/livetribe-slp/raw/master/doc/images/codehaus.gif)