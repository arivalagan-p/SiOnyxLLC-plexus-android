package com.dome.librarynightwave.model.repository.pojo;

public class OpsinRTPStats {
    private int opsin_Fps,opsin_frame_size,opsin_out_of_order, opsin_rtp_received;

    public int getOpsin_Fps() {
        return opsin_Fps;
    }

    public void setOpsin_Fps(int opsin_Fps) {
        this.opsin_Fps = opsin_Fps;
    }

    public int getOpsin_frame_size() {
        return opsin_frame_size;
    }

    public void setOpsin_frame_size(int opsin_frame_size) {
        this.opsin_frame_size = opsin_frame_size;
    }

    public int getOpsin_out_of_order() {
        return opsin_out_of_order;
    }

    public void setOpsin_out_of_order(int opsin_out_of_order) {
        this.opsin_out_of_order = opsin_out_of_order;
    }

    public int getOpsin_rtp_received() {
        return opsin_rtp_received;
    }

    public void setOpsin_rtp_received(int opsin_rtp_received) {
        this.opsin_rtp_received = opsin_rtp_received;
    }
}
