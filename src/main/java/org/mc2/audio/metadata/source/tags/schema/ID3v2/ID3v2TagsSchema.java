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

package org.mc2.audio.metadata.source.tags.schema.ID3v2;

import java.util.AbstractMap.SimpleImmutableEntry;
import org.mc2.audio.metadata.impl.ID3V2MetadataOrigin;
import org.mc2.audio.metadata.API.MetadataOrigin;
import java.util.ArrayList;
import java.util.Iterator;

import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.datatype.Pair;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.ID3v22FieldKey;
import org.jaudiotagger.tag.id3.ID3v22Frames;
import org.jaudiotagger.tag.id3.ID3v22Tag;
import org.jaudiotagger.tag.id3.ID3v23FieldKey;
import org.jaudiotagger.tag.id3.ID3v23Frames;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.ID3v24FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.AbstractFrameBodyNumberTotal;
import org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyIPLS;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTIPL;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUFID;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUnsupported;
import org.jaudiotagger.tag.id3.framebody.FrameBodyWXXX;
import org.jaudiotagger.tag.id3.valuepair.ID3NumberTotalFields;
import org.mc2.audio.metadata.API.Metadata;
import org.mc2.audio.metadata.impl.MetadataDefaultImpl;
import org.mc2.audio.metadata.source.tags.TagsSource;
import org.mc2.audio.metadata.source.tags.schema.TagSchema;

/**
 *
 * @author marco
 */
public class ID3v2TagsSchema extends TagSchema  {
    
    private final AbstractID3v2Tag abstractId3v2Tag;

    public ID3v2TagsSchema(AbstractID3v2Tag abstractId3v2Tag, TagsSource source) {
        super(source);
        this.abstractId3v2Tag=abstractId3v2Tag;
    }

    public ArrayList<Metadata> getExistingMetadata(){
        
        ArrayList<Metadata> out = new ArrayList<>();
        
        for (FieldKey fieldKey : FieldKey.values()) {
            
            Metadata  metadata = this.getPrettyMetadata(fieldKey);
            if (metadata.isEmpty()){
                
                MetadataOrigin origin =  this.getMetadataOrigin(this.source.getSourceId(), fieldKey);
                if (origin != null){
                    metadata = new MetadataDefaultImpl(fieldKey.name(), origin);   
                }
            }
            out.add(metadata);
        }
        return out;
    }
    /* list all the metadata by the Audiofile in the 'generic' FieldKey + values format.
     * MetadataDefaultImpl for fieldKeys with invalid value are reported and value is in the form:
     *
     * [TAGFIELD:SUBID] tagValue (note that tagValue contains the Type= label and value is quoted by "").
     *  
     * Keys not in FieldKey enumerator are reported too, key is [TAGFIELD:SUBID]
     */
    public ArrayList<Metadata> getMetadata(){
       
        ArrayList<Metadata> out = new ArrayList<>();
        ArrayList<TagField> considered = new ArrayList<>();
        
         for (FieldKey fieldKey : FieldKey.values()) {
            
            Metadata pretty = this.getPrettyMetadata(fieldKey);
            
            MetadataOrigin origin = this.getMetadataOrigin(source.getSourceId(), fieldKey);

            if (origin != null && origin instanceof ID3V2MetadataOrigin){           
                considered.addAll(((ID3V2MetadataOrigin)origin).getValidatedTagFields());
                considered.addAll(((ID3V2MetadataOrigin)origin).getDiscardedTagField());  
                considered.addAll(((ID3V2MetadataOrigin)origin).getInvalidTagField()); 
            }
    
            if (pretty.isEmpty() && origin != null){
                
                /* In case of valid FieldKey but Invalid value found (i.e. TRCK Text="1/b )
                *  create a 'nasty' metadata for FieldKey.
                */
                
                Metadata nasty = new MetadataDefaultImpl(fieldKey.name(), origin);
                if  (!nasty.isEmpty()){
                    out.add(nasty);
                }
            
            }else if (!pretty.isEmpty()){
                
                out.add(pretty);     
            }
        }

        Iterator<TagField> iterator = source.geTagFields().iterator();
        while(iterator.hasNext()) {
            
            TagField tagField = iterator.next();
            
            if (!considered.contains(tagField)){
                
                considered.add(tagField);
                Metadata metadata= getMetadata(source.getSourceId(), tagField);
                if (!metadata.isEmpty()){
                    out.add(metadata);
                }
                
            }
        }
        return out;
    }

    /*
    Convert any Valuepair into a METADATA.

    */
    public Metadata getMetadata (String source, TagField tagfield) {
         
        ArrayList<TagField> filteredList = new ArrayList<>();
        ArrayList<TagField> dischargedList = new ArrayList<>();
        ArrayList<TagField> invalidList = new ArrayList<>();
        
        SimpleImmutableEntry<String,String> valuePair = getValuePair(tagfield);
        
        ID3FrameAndSubId frameAndSubId= new ID3FrameAndSubId(valuePair.getKey(), tagfield.getId(), valuePair.getKey() );

        if (!valuePair.getKey().isEmpty() || !valuePair.getValue().isEmpty()){
            
            dischargedList.add(tagfield);
            ID3V2MetadataOrigin origin= new ID3V2MetadataOrigin (source,  frameAndSubId,  filteredList,  dischargedList, invalidList);
            return new MetadataDefaultImpl(valuePair.getKey().toUpperCase(),origin);
            
        } else{

            ID3V2MetadataOrigin origin= new ID3V2MetadataOrigin (source,  frameAndSubId,  filteredList,  dischargedList, invalidList);
            return new MetadataDefaultImpl("[?]",origin);
        }
     }
    /*
    Convert any user defined tagField into a Valuepair, using the subFieldId as Key and
    the description as value.

    */
    public static SimpleImmutableEntry<String,String> getValuePair(TagField tagfield){
        
        AbstractTagFrameBody frameBody = ((AbstractID3v2Frame) tagfield).getBody();
        
        String subFieldId="";
        String value="";
        
        if (frameBody instanceof FrameBodyTXXX){
        
            subFieldId = ((FrameBodyTXXX)frameBody).getDescription();
            value = ((FrameBodyTXXX)frameBody).getFirstTextValue();
        
        } else if (frameBody instanceof FrameBodyWXXX){
            
            subFieldId = ((FrameBodyWXXX)frameBody).getDescription();
            value = ((FrameBodyWXXX)frameBody).getFirstUrlLink();
        
        } else if (frameBody instanceof FrameBodyCOMM){
            
            subFieldId = ((FrameBodyCOMM)frameBody).getDescription();
            value = ((FrameBodyCOMM)frameBody).getFirstTextValue();
        }
        return new SimpleImmutableEntry<>(subFieldId,value);
        
    }
    /*
     Given a FieldKey, search into TagFields the one that should be considered and returns a
     MetadataOrigin builded over this TagField.
    
     jaudiotagger Tag.getFields(FieldKey) silently discards tags with wrong value when building 
     the list, this reports ArrayList<TagField> dischargedList in the created MetadataOrigin
     object.

    */
    public MetadataOrigin getMetadataOrigin(String source, FieldKey fieldKey) {
        
        
        ID3FrameAndSubId formatKey;
        ArrayList<TagField> list= new ArrayList<>();
        String subFieldId;
        ArrayList<TagField> filteredList = new ArrayList<>();
        ArrayList<TagField> dischargedList = new ArrayList<>();
        ArrayList<TagField> invalidList = new ArrayList<>();

        try {
            formatKey = getId3KeyFromFieldKey(fieldKey);
            list = (ArrayList) abstractId3v2Tag.getFields(formatKey.getFrameId());
            subFieldId = formatKey.getSubId();
        } catch (NullPointerException ex){
        
            return null;
        }

        if (subFieldId != null) {
            
            for (TagField tagfield : list) {

                AbstractTagFrameBody next = ((AbstractID3v2Frame) tagfield).getBody();
                if (next instanceof FrameBodyTXXX)
                {
                    if (((FrameBodyTXXX) next).getDescription().equals(subFieldId))
                    {
                        filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                }
                else if (next instanceof FrameBodyWXXX)
                {
                    if (((FrameBodyWXXX) next).getDescription().equals(subFieldId))
                    {
                         filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                }
                else if (next instanceof FrameBodyCOMM)
                {
                    if (((FrameBodyCOMM) next).getDescription().equals(subFieldId))
                    {
                        filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                }
                else if (next instanceof FrameBodyUFID)
                {
                    if (((FrameBodyUFID) next).getOwner().equals(subFieldId))
                    {
                        filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                }
                else if (next instanceof FrameBodyIPLS)
                {
                    boolean found=false;
                    for (Pair entry : ((FrameBodyIPLS) next).getPairing().getMapping())
                    {
                        if (entry.getKey().equals(subFieldId))
                        {
                             found=true;
                        }
                    }
                    
                    if (found){
                        filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                    
                }
                else if (next instanceof FrameBodyTIPL)
                {
                    boolean found=false;
                    for (Pair entry : ((FrameBodyTIPL) next).getPairing().getMapping())
                    {
                        if (entry.getKey().equals(subFieldId))
                        {
                            found=true;
                        }
                    }
                    if (found){
                        filteredList.add(tagfield);
                    } else {
                        //dischargedList.add(tagfield);
                    }
                }
                else if (next instanceof FrameBodyUnsupported) {
                    
                    filteredList = list;
                    break;
                }
                else
                {
                    throw new RuntimeException("Need to implement getFields(FieldKey genericKey) for:" + next.getClass());
                } 
            }
        
        } else if(ID3NumberTotalFields.isNumber(fieldKey)) {
            
            for (TagField tagfield : list) {
                
                AbstractTagFrameBody next = ((AbstractID3v2Frame) tagfield).getBody();
                
                if (next instanceof AbstractFrameBodyNumberTotal)
                {
                    if (((AbstractFrameBodyNumberTotal) next).getNumber() != null)
                    {
                        filteredList.add(tagfield);
                    } else {
                        invalidList.add(tagfield);
                    }
                }
            }
        }
        else if(ID3NumberTotalFields.isTotal(fieldKey)) {
            
            for (TagField tagfield : list) {
                AbstractTagFrameBody next = ((AbstractID3v2Frame) tagfield).getBody();
                if (next instanceof AbstractFrameBodyNumberTotal)
                {
                    if (((AbstractFrameBodyNumberTotal) next).getTotal() != null)
                    {
                        filteredList.add(tagfield);
                    } else {
                        invalidList.add(tagfield);
                    }
                }
            }
        }
        else {
            filteredList = list;
        }
        
        return new ID3V2MetadataOrigin(source,  formatKey,  filteredList,  dischargedList, invalidList);

    }

    public ID3FrameAndSubId getId3KeyFromFieldKey(FieldKey fieldKey) {
        
        ID3FrameAndSubId frameAndSubId=null;
        
        if (abstractId3v2Tag instanceof  ID3v24Tag){
            ID3v24FieldKey Id3v2Key = ID3v24Frames.getInstanceOf().getId3KeyFromGenericKey(fieldKey);
            frameAndSubId= new ID3FrameAndSubId(fieldKey.name(), Id3v2Key.getFrameId(),Id3v2Key.getSubId() );
            
        }

        else if (abstractId3v2Tag instanceof  ID3v23Tag){
            ID3v23FieldKey Id3v2Key = ID3v23Frames.getInstanceOf().getId3KeyFromGenericKey(fieldKey);
            frameAndSubId= new ID3FrameAndSubId(fieldKey.name(), Id3v2Key.getFrameId(),Id3v2Key.getSubId() );
            
            

        }

         else if (abstractId3v2Tag instanceof  ID3v22Tag){
            ID3v22FieldKey Id3v2Key = ID3v22Frames.getInstanceOf().getId3KeyFromGenericKey(fieldKey);
            frameAndSubId= new ID3FrameAndSubId(fieldKey.name(), Id3v2Key.getFrameId(),Id3v2Key.getSubId() );
            
        }
        
        return frameAndSubId;
    }
}
