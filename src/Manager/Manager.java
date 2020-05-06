package Manager;
import org.libvirt.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.StreamGobbler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.text.SimpleDateFormat;
public class Manager {
	public static String DEFAULTCHARTSET = "UTF-8";
	public static Map<String, String> account = new HashMap<String, String>();
	public static Map<Integer, Float> domainMemory = new HashMap<Integer, Float>();
	public static Map<String, Integer> hostPower = new HashMap<String, Integer>();
	public static Connect conn = null;
	public static JTextArea textArea = new JTextArea();
	// 创建文本框，指定可见列数为8列
	public static final JTextField sourceId = new JTextField(15);
	public static final JTextField targetId = new JTextField(15);
	public static final JTextField domainId = new JTextField(15);
	public static JLabel result = new JLabel();
	public static JLabel migrateResult = new JLabel();
	public static void main(String[] args) {
		addAccount();
		createPage();
		getHostInfo();
    }
	
	static private void createPage() {
		JFrame jf = new JFrame("Virtual Machine Manager");
        jf.setSize(800, 800);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        textArea.setLineWrap(false);                       // 自动换行
        
        JButton btn = new JButton("commit");
        JButton migrateBtn = new JButton("migrate");
        JLabel hostId = new JLabel("host id");
        JLabel targrtLabel = new JLabel("target id");
        JLabel migrateId = new JLabel("domain name");
        panel.add(hostId);
        panel.add(sourceId);
        try{
        	localConnect(btn);
        	migrateBtn(migrateBtn);
      	} catch (LibvirtException e) {
          	System.out.println("exception caught:"+e);
          	System.out.println(e.getError());
      	}
        panel.add(btn);
        panel.add(targrtLabel);
        panel.add(targetId);
        panel.add(migrateId);
        panel.add(domainId);
        panel.add(migrateBtn);
        panel.add(result);
        panel.add(textArea);
        panel.add(migrateResult);

        jf.setContentPane(panel);
        //jf.setContentPane(scrollPane);
        jf.setVisible(true);
	}
	
	//connect and get virtual machines
    static private void localConnect(JButton btn) throws LibvirtException {
    	btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try{
            		textArea.setText("");
            		conn = new Connect("qemu+tcp://"+sourceId.getText()+"/system");
            		result.setText("connect succuss! ");
            		textArea.append("hostName: " + conn.getHostName() + "\n");
            		textArea.append("连接到的宿主机的剩余内存：" + conn.getFreeMemory()/1024 + "KB\n");
            		textArea.append("连接到的宿主机的最大Cpu number：" + conn.getMaxVcpus(null) + "\n");
                    int[] idsOfDomain = conn.listDomains();
                    textArea.append("running virtual machine: " + idsOfDomain.length + "\n");
            		for (int id : idsOfDomain) {
                        Domain domain = conn.domainLookupByID(id);
                        textArea.append("虚拟机的id:"+ domain.getID() + "\n"); 
                        textArea.append("虚拟机的uuid:"+ domain.getUUIDString() + "\n");
                        textArea.append("虚拟机的名称:"+ domain.getName() + "\n");
                        textArea.append("虚拟机的状态:"+ domain.getInfo().state + "\n");
                        textArea.append("虚拟机的memory usage:"+ getDomainMem(domain.getID())+ "%\n");
                        //cpu availability
                        textArea.append("虚拟机的cpu usage:"+ getDomainCPU(domain) + "%\n");
                    }
            	} catch (LibvirtException er) {
            		System.out.println("exception caught:"+er);
            		System.out.println(er.getError());
            		result.setText("exception caught:"+er.getError());
            	}
            }
        });
    }
    
    static private void migrateBtn(JButton migrateId) throws LibvirtException {
    	migrateId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try {
            		Domain domain = conn.domainLookupByID(Integer.parseInt(domainId.getText()));
            		migrate(sourceId.getText(), targetId.getText(), domain);
            	} catch (LibvirtException er) {
            		System.out.println(er.getError());
            		result.setText("exception caught:" + er.getError());
            	}
            }
        });
    }

    static private void migrate(String source, String target, Domain domain) throws LibvirtException {
    	try {
    		if(!source.equals("")) {
    			Connection connection1 = new Connection(source);
        		connection1.connect();// 连接
        		connection1.authenticateWithPassword("root", account.get(source));// 认证
    			Session session1 = connection1.openSession();// 打开一个会话
    			session1.execCommand("hostname source");// 执行命令
    			connection1.close();
    			session1.close();
    		} else {
    			Runtime rt = Runtime.getRuntime();
    			try {
    				rt.exec("hostname source");
    			} catch (IOException err) {
    				err.printStackTrace();
    			}
    		}
    		if(!targetId.getText().equals("")) {
    			Connection connection2 = new Connection(target);
        		connection2.connect();// 连接
        		connection2.authenticateWithPassword("root", account.get(target));// 认证
    			Session session2 = connection2.openSession();// 打开一个会话
    			session2.execCommand("hostname target");// 执行命令
    			connection2.close();
    			session2.close();
    		} else {
    			Runtime rt = Runtime.getRuntime();
    			try {
    				rt.exec("hostname target");
    			} catch (IOException err) {
    				err.printStackTrace();
    			}
    		}
			
    	} catch (IOException er) {
			er.printStackTrace();
		}
    	try{
    		Connect sourceCon = new Connect("qemu+tcp://"+source+"/system");
    		//Domain domain = sourceCon.domainLookupByID(Integer.parseInt(domainId.getText()));
    		Connect targetCon = new Connect("qemu+tcp://"+target+"/system", false);
    		domain.migrate(targetCon, 1<<9, null, null, 0);
    		migrateResult.setText("migrate succuss!");
    	} catch (LibvirtException er) {
    		System.out.println("exception caught:"+er);
    		System.out.println(er.getError());
    		result.setText("exception caught:" + er.getError());
    	}
    }
    
    static private void addAccount() {
    	account.put("10.1.18.137", "Qyl330182");
    	account.put("10.1.18.248", "me521..");
    }
    
    static private void getHostInfo() {
    	String s;
		Process p;
		String cmd_ipmi="";
		cmd_ipmi="ipmitool sdr list";
		
		int start = 0, end = 0;
		int syspower = 0;
        String tmp = "";
        //SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
       
        for (int i = 0; i < 15000; i++) {
            try {
                p = Runtime.getRuntime().exec(cmd_ipmi);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null) {
                    if (s.indexOf("System Power") != -1 || s.indexOf("Total_Power") != -1) {
                        start = s.indexOf("|") + 1;
                        end = s.lastIndexOf("|");
                        tmp = s.substring(start, end).replaceAll("Watts", "").trim();
                        syspower = Integer.parseInt(tmp);
                    	hostPower.put("10.1.18.248", syspower);
                    	System.out.println("System Power=	" + syspower + "	Watts");
                    }
                }
            } catch (Exception e) {
            	System.out.println(e);
            }
        	try {
            	Connection connection2 = new Connection("10.1.18.137");
    			connection2.connect();// 连接
    			connection2.authenticateWithPassword("root", account.get("10.1.18.137"));// 认证
    			Session session2 = connection2.openSession();// 打开一个会话
    			session2.execCommand(cmd_ipmi);// 执行命令
    			String res = processStdout(session2.getStdout(), DEFAULTCHARTSET);
                syspower = Integer.parseInt(res);
                hostPower.put("10.1.18.137", syspower);
    			System.out.println(res);
    			connection2.close();
    			session2.close();
            } catch (IOException er) {
    			er.printStackTrace();
    		}
        	Iterator <Map.Entry<String, Integer>> entries= hostPower.entrySet().iterator();
            int minPower=10000;
            String min="", over="";
        	while(entries.hasNext()) {
            	Map.Entry<String, Integer> entry = entries.next();
            	if(entry.getValue()>100) over=entry.getKey();
            	if(entry.getValue()<minPower) min = entry.getKey();
            	try {
                	minimizePower(over, min);
            	} catch (LibvirtException er) {}
            	//System.out.println(entry.getValue());
            	break;
            }
        }
    }
    
    static private double getDomainMem (int id) {
    	Process p;
    	Double available=0.0;
    	Double unused=0.0;
    	double util_mem=0.0;
    	String s;
		String cmd_ipmi="";
		cmd_ipmi="virsh dommemstat "+id;
		
		try {
          p = Runtime.getRuntime().exec(cmd_ipmi);
          BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
          while ((s = br.readLine()) != null) {
              if (s.indexOf("available") != -1 ) {
            	  available = Double.parseDouble(s.split(" ")[1]);
              } else if(s.indexOf("unused") != -1) {
            	  unused = Double.parseDouble(s.split(" ")[1]);
              }
          }
          util_mem = ((available-unused)/available)*100;
	      } catch (Exception e) {
	
	      }
		return util_mem;
    }
    
    static private double getDomainCPU(Domain domain) {
    	double usage=0.0;
    	try {
    		Date t1 = new Date();
	        long c1 = domain.getInfo().cpuTime;
	        try {
	        	Thread.sleep(50);
	        } catch (InterruptedException err) {
	        	System.out.println(err);
	        }
	        Date t2 = new Date();
	        long c2 = domain.getInfo().cpuTime;
	        int c_nums = domain.getInfo().nrVirtCpu;
	        usage = (c2-c1)*100/((t2.getTime()-t1.getTime())*c_nums*1e9);
    	} catch (LibvirtException er) {}
    	return usage;
    }
    
    static private void minimizePower(String overPoint, String minPoint) throws LibvirtException {
    	Connect over = null;
    	int[] idsOfDomain = null;
    	int domainMinId = 0;
    	Domain minDomain = null;
		double min=100.0;
    	DomainList [] domainList = new DomainList[10];
    	if(overPoint.equals("10.1.18.248")) {
    		over = conn;
    	} else {
    		over =  new Connect("qemu+tcp://"+overPoint+"/system");
    	}
		idsOfDomain = over.listDomains();
		int i=0;
    	for (int id : idsOfDomain) {
            Domain domain = conn.domainLookupByID(id);
            domainList[i]=new DomainList(domain.getID(),getDomainMem(domain.getID()),getDomainCPU(domain));
            i++;
        }
    	for(int j = 0; j < idsOfDomain.length; j++) {
    		if(domainList[j].getMem()<min) {
    			min = domainList[j].getMem();
    			domainMinId=domainList[j].getId();
    		}
    	}
		minDomain = conn.domainLookupByID(domainMinId);
		migrate(overPoint, minPoint, minDomain);
    }
    
    public static String processStdout(InputStream in, String charset){
		InputStream stdout = new StreamGobbler(in);
		StringBuffer buffer = new StringBuffer();
		int start = 0, end = 0;
        String tmp = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout, charset));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("System Power") != -1 || line.indexOf("Total_Power") != -1) {
					start = line.indexOf("|") + 1;
					end = line.lastIndexOf("|");
	                tmp = line.substring(start, end).replaceAll("Watts", "").trim();
					buffer.append(tmp);
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toString();
    }
}
