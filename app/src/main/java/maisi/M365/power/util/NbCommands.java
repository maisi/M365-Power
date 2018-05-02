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

public enum NbCommands {
    MASTER_TO_M365(0x20),
    MASTER_TO_BATTERY(0x22),
    READ(0x01),
    WRITE(0x03);

    private final int command;

    NbCommands(int command) {
        this.command = command;
    }

    public int getCommand() {
        return this.command;
    }

}
