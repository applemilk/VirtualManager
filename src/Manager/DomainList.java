package Manager;

public class DomainList {
	protected int id;
	protected double cpu;
	protected double memory;
	
	public DomainList(int id, double cpu, double memory) {
		this.id = id;
		this.cpu = cpu;
		this.memory = memory;
	}
	
	public double getCPU() {
		return cpu;
	}
	
	public double getMem() {
		return memory;
	}
	
	public int getId() {
		return id;
	}
	
	public void setCPU(double cpu) {
		this.cpu = cpu;
	}
	
	public void setMem(double mem) {
		this.memory = mem;
	}
}
