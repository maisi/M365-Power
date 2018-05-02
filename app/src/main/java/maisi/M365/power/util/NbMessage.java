/*****************************************************************************/
//	Function: M365 BLE message builder
//	Author:   Salvador Mart�n
//	Date:    12/02/2018
//
//	This library is free software; you can redistribute it and/or
//	modify it under the terms of the GNU Lesser General Public
//	License as published by the Free Software Foundation; either
//	version 2.1 of the License, or (at your option) any later version.
//
//	This library is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//	Lesser General Public License for more details.
//
//	I am not responsible of any damage caused by the misuse of this library.
//	Use at your own risk.
//
//	If you modify or use this, please don't delete my name and give me credits.
/*****************************************************************************/

package maisi.M365.power.util;

import java.util.ArrayList;
import java.util.List;

public class NbMessage {
    private List<Integer> msg;

    private int direction;
    private int rw;
    private int position;
    private List<Integer> payload;
    private int checksum;

    public NbMessage() {
        direction = 0;
        rw = 0;
        position = 0;
        payload = null;
        checksum = 0;
    }

    public NbMessage setDirection(NbCommands drct) {
        direction = drct.getCommand();
        checksum += direction;

        return this;
    }

    public NbMessage setRW(NbCommands readOrWrite) { // read or write
        rw = readOrWrite.getCommand();
        checksum += rw;

        return this;
    }

    public NbMessage setPosition(int pos) {
        position = pos;
        checksum += position;

        return this;
    }

    public NbMessage setPayload(byte[] bytesToSend) {
        this.payload = new ArrayList<>();

        checksum += bytesToSend.length + 2;

        for (int b : bytesToSend) {
            this.payload.add(b);
            checksum += b;
        }

        return this;
    }

    public NbMessage setPayload(List<Integer> bytesToSend) {
        payload = bytesToSend;

        checksum += payload.size() + 2;

        for (int i : payload) {
            checksum += i;
        }
        return this;
    }

    public NbMessage setPayload(int singleByteToSend) {
        payload = new ArrayList<>();
        payload.add(singleByteToSend);

        checksum += 3;
        checksum += singleByteToSend;

        return this;
    }

    public String build() {
        setupHeaders();

        setupBody();

        calculateChecksum();

        return construct();
    }

    private void setupHeaders() {
        msg = new ArrayList<>(0);

        msg.add(0x55);
        msg.add(0xAA);
    }

    private void setupBody() {
        msg.add(payload.size() + 2);
        msg.add(direction);
        msg.add(rw);
        msg.add(position);

        for (Integer i : payload) {
            msg.add(i);
        }
    }

    private void calculateChecksum() {
        checksum ^= 0xffff;

        msg.add((checksum & 0xff));
        msg.add(checksum >> 8);
    }

    private String construct() {
        String result = "";
        for (Integer i : msg) {
            result += (i >= 0) && (i <= 15) ? "0" + Integer.toHexString(i) : Integer.toHexString(i);
        }
        return result;
    }
}