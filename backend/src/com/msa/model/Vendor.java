package com.msa.model;

public class Vendor {

    private int vendorId;
    private String vendorName;
    private String address;
    private String contactNo;

    public Vendor() {}

    public Vendor(String vendorName, String address, String contactNo) {
        this.vendorName = vendorName;
        this.address = address;
        this.contactNo = contactNo;
    }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }
}
