/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.strange.cqc;

import com.gluonhq.strange.Gate;
import com.gluonhq.strange.gate.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CQCSession {

    private Socket socket;
    private OutputStream cqcOutputStream;
    private InputStream cqcInputStream;
    private final short appId;
    private final String name;

    public CQCSession() {
        this ("anonymous");
    }

    public CQCSession(String name) {
        appId = getNextAppId();
        this.name = name;
    }

    public CQCSession(String name, short id) {
        appId = id;
        this.name = name;
    }

    public void connect(String host, int port) throws IOException {
        connect(host, port, -1);
    }

    public void connect(String host, int cqcPort, int appPort) throws IOException {
        System.err.println("Connecting to "+host+":"+cqcPort);
        Socket socket = new Socket(host, cqcPort);
        System.err.println("socket created: "+socket);
        this.socket = socket;
        this.cqcOutputStream = socket.getOutputStream();
    }

    public void sendHello() throws IOException {
        sendCqcHeader(Protocol.CQC_TP_HELLO, 0);
    }

    public int createQubit() throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 4);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) 0, Protocol.CQC_CMD_NEW, option);
        ResponseMessage msg = readMessage();
        ResponseMessage done = readMessage();
        return msg.getQubitId();
    }

    public int receiveQubit() throws IOException{
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 4);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) 0, Protocol.CQC_CMD_RECV, option);
        ResponseMessage msg = readMessage();
        ResponseMessage done = readMessage();
        return msg.getQubitId();
    }

    public void sendQubit(int qid, short port) throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 12);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) qid, Protocol.CQC_CMD_SEND, option);
        sendCommunicationHeader((short)0, port, 127*256*256*256+1);
        ResponseMessage done = readMessage();
        if (Protocol.CQC_TP_DONE != done.getType()) {
            throw new IOException("Send should return done!");
        }
    }

    // TODO return an EPR (create that class first)
    public ResponseMessage createEPR(String name, short port) throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 12);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) 0, Protocol.CQC_CMD_EPR, option);
        sendCommunicationHeader((short)0, port, 127*256*256*256+1);
        ResponseMessage msg = readMessage();
        ResponseMessage done = readMessage();
        return msg;
    }

    public ResponseMessage receiveEPR() throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 4);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) 0, Protocol.CQC_CMD_EPR_RECV, option);
        ResponseMessage msg = readMessage();
        ResponseMessage done = readMessage();
        return msg;
    }

    public void applyGate(Gate gate) throws IOException {
        int qid = gate.getMainQubitIndex();
        byte cmdByte = 0;
        int len = 4;
        if (gate instanceof X) {
            cmdByte = Protocol.CQC_CMD_X;
        } else if (gate instanceof Hadamard) {
            cmdByte = Protocol.CQC_CMD_H;
        } else if (gate instanceof Cnot) {
            cmdByte = Protocol.CQC_CMD_CNOT;
            len = 6;
        }
        sendCqcHeader(Protocol.CQC_TP_COMMAND, len);

        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        System.err.println("Send command to apply gate");
        sendCommandHeader((short) qid, cmdByte, option);
        if (gate instanceof Cnot) {
            sendExtraQubitHeader((short) ((Cnot) gate).getSecondQubitIndex());
        }
        System.err.println("wait for TP_DONE");
        ResponseMessage done = readMessage();
        System.err.println("that was a message of type "+done.getType());
        if (done.getType() != Protocol.CQC_TP_DONE) {
            System.err.println("That wasn't TP_DONE!");
            throw new IOException("Got message of type "+done.getType()+" instead of TP_DONE");
        }
    }

    public boolean measure(int qid) throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 8);
        byte option = Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) qid, Protocol.CQC_CMD_MEASURE, option);
        sendCQCAssignHeader(qid);
        ResponseMessage done = readMessage();
        return done.getMeasurement();
    }

    public ResponseMessage readMessage() throws IOException {
        if (cqcInputStream == null) {
            System.err.println("IS NULL!!!\n");
            cqcInputStream = socket.getInputStream();
        }
        ResponseMessage answer = new ResponseMessage(cqcInputStream);
        return answer;
    }

    public void releaseQubit(int qid) throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, 4);
        byte option = Protocol.CQC_OPT_NOTIFY | Protocol.CQC_OPT_BLOCK;
        sendCommandHeader((short) qid, Protocol.CQC_CMD_RELEASE, option);
        ResponseMessage answer = new ResponseMessage(cqcInputStream);
        System.err.println("Done reading message for releasequbit "+name);
    }

    static short appIdCounter = 0;

    private static synchronized short getNextAppId() {
        appIdCounter++;
        return appIdCounter;
    }
    /*
     * len = exclusive header.
     */
    private void sendCqcHeader(byte type, int len) throws IOException {
        int totalLength = len + 8;

        DataOutputStream dos = new DataOutputStream(cqcOutputStream);
        dos.writeByte(Protocol.VERSION);
        dos.writeByte(type);
        dos.writeShort(appId);
        dos.writeInt(len);
        dos.flush();
    }

    private void sendCommandHeader(short qubit_id, byte command, byte option) throws IOException {
        DataOutputStream dos = new DataOutputStream(cqcOutputStream);
        dos.writeShort(qubit_id); // qubit_id
        dos.writeByte(command);
        dos.writeByte(option);
        dos.flush();
    }

    private void sendCommunicationHeader(short appId, short port, int host) throws IOException {
        DataOutputStream dos = new DataOutputStream(cqcOutputStream);
        dos.writeShort(appId);
        dos.writeShort(port);
        dos.writeInt(host);
        dos.flush();
    }

    private void sendExtraQubitHeader(short extraQubit) throws IOException {
        DataOutputStream dos = new DataOutputStream(cqcOutputStream);
        dos.writeShort(extraQubit);
        dos.flush();
    }

    private void sendCQCAssignHeader(int refId) throws IOException {
        DataOutputStream dos = new DataOutputStream(cqcOutputStream);
        dos.writeInt(refId);
        dos.flush();
    }

    private void sendCommand(byte command, short qubit_id, boolean notify, boolean action, boolean block, int length) throws IOException {
        sendCqcHeader(Protocol.CQC_TP_COMMAND, length);
    }

    private void sendSimpleCommand(byte command, short qid) throws IOException {
        sendCommand(command, qid, false, false, false,0);
        System.err.println("Sending simple command "+command);
    }

}
