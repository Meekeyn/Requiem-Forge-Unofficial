package ua.meekeyn.requiemunofficial.common.soul;

public class PlayerSoulData {

    private int soulPower;
    private int ichorLevel;
    private SoulState state;

    public PlayerSoulData() {
        this.soulPower = 0;
        this.ichorLevel = 0;
        this.state = SoulState.MORTAL;
    }

    public int getSoulPower() {
        return soulPower;
    }

    public void setSoulPower(int soulPower) {
        this.soulPower = soulPower;
    }

    public void addSoulPower(int amount) {
        this.soulPower += amount;
    }

    public int getIchorLevel() {
        return ichorLevel;
    }

    public void setIchorLevel(int ichorLevel) {
        this.ichorLevel = ichorLevel;
    }

    public void addIchor(int amount) {
        this.ichorLevel += amount;
    }

    public SoulState getState() {
        return state;
    }

    public void setState(SoulState state) {
        this.state = state;
    }
}
