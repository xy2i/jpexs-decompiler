/*
 *  Copyright (C) 2010-2014 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.types.BasicType;
import com.jpexs.decompiler.flash.types.annotations.SWFType;
import java.io.IOException;

public class DefineFontNameTag extends Tag {

    @SWFType(BasicType.UI16)
    public int fontId;
    public String fontName;
    public String fontCopyright;
    public static final int ID = 88;

    public DefineFontNameTag(SWFInputStream sis, long pos, int length) throws IOException {
        super(sis.getSwf(), ID, "DefineFontName", pos, length);
        fontId = sis.readUI16("fontId");
        fontName = sis.readString("fontName");
        fontCopyright = sis.readString("fontCopyright");
    }
}
