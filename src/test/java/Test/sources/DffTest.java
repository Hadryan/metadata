/*
 * Library for manipulating metadata from Audiofiles and cue sheets.
 *
 * Copyright (C) 2017 Marco Curti (marcoc1712 at gmail dot com).
 *
 * Based upon (and depends on):
 * 
 * - cueLib by Jan-Willem van den Broek
 * - jaudiotagger:audio tagging library Copyright (C) 2015 Paul Taylor
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package Test.sources;

import Test.utils.TestUtils;
import java.io.PrintStream;
import org.junit.Before;
import org.junit.Test;
import org.mc2.audio.metadata.API.exceptions.InvalidAudioFileException;
import org.mc2.audio.metadata.API.exceptions.InvalidAudioFileFormatException;
import org.mc2.audio.metadata.source.tags.file.Dff;

public class DffTest {
    @Before
    public void setUp() throws Exception {
        
        System.setOut(new PrintStream(System.out, true, "utf-8"));
        
    }
    @Test
    public void TestRead() throws Exception{

        String directory = "F:/SVILUPPO/01 - SqueezeboxServer Plugins/musica campione/provaDff";
        String filename = "dff_02-822-400 (dsd64)_TAG.dff";
        String path = directory+"/"+filename;
        
        try{
            Dff audiofile = (Dff)org.mc2.audio.metadata.source.tags.file.AudioFile.get(path);
            TestUtils.printAudioFile(audiofile);
 
        } catch (InvalidAudioFileException | InvalidAudioFileFormatException ex){

            System.out.println("Invalid Audiofile:"+ex.getMessage());
        
        }
       
         
    }
}
