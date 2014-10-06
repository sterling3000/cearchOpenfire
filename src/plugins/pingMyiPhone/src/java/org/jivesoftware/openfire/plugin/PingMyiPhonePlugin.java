package org.jivesoftware.openfire.plugin;

//import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import java.sql.Timestamp;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class PingMyiPhonePlugin implements Plugin{

	private PacketInterceptor packetInterceptor;
	
	//private IQRouter iqRouter;
	private MessageRouter messageRouter;
	private GroupManager groupManager = GroupManager.getInstance();
	private UserManager userManager = UserManager.getInstance();
	private PresenceManager presenceManager = XMPPServer.getInstance().getPresenceManager();
	private UserAuthListener listener;
	private Timer timer;
	private String serverName;
    private JID serverAddress;
	
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		
		serverName = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
        serverAddress = new JID(serverName);
        
        System.out.print("Starting PingMyiPhone plugin...`" + "\n");
        
		//iqRouter = XMPPServer.getInstance().getIQRouter();
		messageRouter = XMPPServer.getInstance().getMessageRouter();
		
		// Start a four minute timer
		timer = new Timer();
		timer.schedule(new MyTimerTask(), 60*4*1000, 60*4*1000);
		
		// Create a listener that is listening to the user auth session event.
		listener = new UserAuthListener();
		SessionEventDispatcher.addListener(listener);
		
		// Register a packet listener so that we can track packet traffic.
        packetInterceptor = new PacketInterceptor() {
			@Override
			public void interceptPacket(Packet packet, Session session,
					boolean incoming, boolean processed)
					throws PacketRejectedException {
				// Only track processed packets so that we don't count them twice.
                if (processed && !incoming) {
                	
                	
                	//System.out.print("//////// PRINTING THE PACKET TO BE SENT //////" + packet.toString() + "\n");
                	//System.out.print("//////// PRINT END ////////" + "\n");
                	
                }
			}
        };
        InterceptorManager.getInstance().addInterceptor(packetInterceptor);
        
	}

	public void destroyPlugin() {
		   InterceptorManager.getInstance().removeInterceptor(packetInterceptor);
		   packetInterceptor = null;
		   timer.cancel();
	}
	
	
	private IQ createPingPacket(JID jid)
	{
		IQ iq = new IQ(IQ.Type.get);
        iq.setTo(jid);
        iq.setFrom(serverAddress);
        
        iq.setChildElement("ping", "urn:xmpp:ping");

        return iq;
        
        //<ping xmlns="urn:xmpp:ping"/>
	}
	
	
	private class UserAuthListener implements SessionEventListener
	{

		@Override
		public void sessionCreated(Session session) {
			// Upon user pass the authentication, we grab the user's JID
			JID jid = session.getAddress();
			String resourceName = jid.getResource();
			
			// We need to check if the resource name is equal to "iOS", if so, we put the user in the iOS group
			// If the resource is "Android" or "Blackberry", we check if this user is previously in the iOS group.
			if (resourceName.equals("iOS"))
			{
				try {
					Group iOS_group = groupManager.getGroup("iOS");
					if (!iOS_group.isUser(jid))
						groupManager.getProvider().addMember("iOS", jid, false);
					
					//System.out.print("Session created for jid :" + jid + "\n");
				
				} catch (GroupNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e)
				{
					System.out.print("Error message: " + e.getMessage());
				}
			}
			else
			{
				try {
					Group iOS_group = groupManager.getGroup("iOS");
					if (iOS_group.isUser(jid))
						groupManager.getProvider().deleteMember("iOS", jid);
				
				} catch (GroupNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//System.out.print("//// Resource name: " + jid.getResource() + " //////\n");
		}

		@Override
		public void sessionDestroyed(Session session) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void anonymousSessionCreated(Session session) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void anonymousSessionDestroyed(Session session) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void resourceBound(Session session) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class MyTimerTask extends TimerTask
	{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			
			try {
				  Collection<JID> members = groupManager.getGroup("iOS").getMembers();
				  Iterator<JID> ir = members.iterator();
				  
                  int cnt = 0;
                  
				  java.util.Date date= new java.util.Date();
				  System.out.print("Start sending ping message at " + new Timestamp(date.getTime()) + "\n");
				  
				  while (ir.hasNext()) {
                      JID jid = ir.next();
                      
                      try {
                    	  
                    	  String node = jid.getNode(); 
                    	  if (node == null)
                    		  continue;
                    	                      	  
                    	  User user = userManager.getUser(node);
                    	  
							Presence userPresence = presenceManager.getPresence(user);
							if (userPresence != null)
							{
								boolean isOnline = userPresence.isAvailable();
								
								if (isOnline)
								{
									Message msg = new Message();
									msg.setFrom(serverAddress);
									msg.setTo(jid);
									msg.setBody("ABC");

									messageRouter.route(msg);
									
									cnt++;
								}
							}
							
						} catch (UserNotFoundException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							System.out.print("User not found: " + jid  + "\n");
						}
						
				  }
				  
				  date= new java.util.Date();
				  System.out.print("End sending ping message at " + new Timestamp(date.getTime()) + ". Sent to " + cnt + " users." + "\n");
              
					
			} catch (GroupNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
				
		}
		
	}
}
