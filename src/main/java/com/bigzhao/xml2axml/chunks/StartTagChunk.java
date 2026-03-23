package com.bigzhao.xml2axml.chunks;

import com.bigzhao.xml2axml.IntWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Roy on 15-10-5.
 */
public class StartTagChunk extends Chunk<StartTagChunk.H>{
    public class H extends Chunk.NodeHeader{

        public H() {
            super(ChunkType.XmlStartElement);
        }
    }

    public String name;
    public String prefix;
    public String namespace;
    public short attrStart=20;
    public short attrSize=20;
    public short idIndex=0;
    public short styleIndex=0;
    public short classIndex=0;
    public LinkedList<AttrChunk> attrs=new LinkedList<AttrChunk>();
    public List<StartNameSpaceChunk> startNameSpace=new Stack<StartNameSpaceChunk>();

    public StartTagChunk(Chunk parent,XmlPullParser p) throws XmlPullParserException {
        super(parent);
        name = p.getName();
        stringPool().addString(name);
        prefix = p.getPrefix();
        namespace = p.getNamespace();
        int ac = p.getAttributeCount();
        for (short i = 0; i < ac; ++i) {
            String prefix = p.getAttributePrefix(i);
            String namespace = p.getAttributeNamespace(i);
            String name = p.getAttributeName(i);
            String val = p.getAttributeValue(i);
            AttrChunk attr = new AttrChunk(this);
            attr.prefix = prefix;
            attr.namespace = namespace;
            attr.rawValue = val;
            attr.name = name;
            stringPool().addString(namespace,name);
            attrs.add(attr);
            if ("id".equals(name)&&"http://schemas.android.com/apk/res/android".equals(namespace)){
                idIndex=i;
            }else if (prefix==null&&"style".equals(name)){
                styleIndex=i;
            }else if (prefix==null&&"class".equals(name)){
                classIndex=i;
            }
        }
        int nsStart = p.getNamespaceCount(p.getDepth() - 1);
        int nsEnd = p.getNamespaceCount(p.getDepth());
        for (int i = nsStart; i < nsEnd; i++) {
            StartNameSpaceChunk snc=new StartNameSpaceChunk(parent);
            snc.prefix = p.getNamespacePrefix(i);
            stringPool().addString(null,snc.prefix);
            snc.uri = p.getNamespaceUri(i);
            stringPool().addString(null,snc.uri);
            startNameSpace.add(snc);
        }
    }

    /**
     * Get the resource ID for an attribute by looking it up in the string pool.
     * Returns -1 if no resource ID is assigned (e.g. non-android namespace).
     */
    private int getAttrResourceId(AttrChunk attr) {
        return stringPool().getResourceId(attr.namespace, attr.name);
    }

    @Override
    public void preWrite() {
        for (AttrChunk a:attrs) a.calc();

        // Sort attributes by resource ID (ascending).
        // Attributes without a resource ID (-1) are placed at the end.
        // This is required by the Android binary XML format, which uses
        // binary search to locate attributes by resource ID.
        Collections.sort(attrs, new Comparator<AttrChunk>() {
            @Override
            public int compare(AttrChunk a, AttrChunk b) {
                int idA = getAttrResourceId(a);
                int idB = getAttrResourceId(b);
                // Treat 0 and -1 (no valid resource ID) as "unknown", sort to the end.
                // Resources.getIdentifier() returns 0 when attr is not found,
                // and getResourceId() returns -1 when not in string pool.
                boolean invalidA = (idA <= 0);
                boolean invalidB = (idB <= 0);
                if (invalidA && invalidB) return 0;
                if (invalidA) return 1;
                if (invalidB) return -1;
                // Use Long comparison to avoid integer overflow with large unsigned IDs
                return Long.compare(idA & 0xFFFFFFFFL, idB & 0xFFFFFFFFL);
            }
        });

        // Recalculate special attribute indices after sorting
        idIndex = 0;
        styleIndex = 0;
        classIndex = 0;
        for (short i = 0; i < attrs.size(); i++) {
            AttrChunk attr = attrs.get(i);
            if ("id".equals(attr.name) && "http://schemas.android.com/apk/res/android".equals(attr.namespace)) {
                idIndex = (short)(i + 1); // 1-based index
            } else if (attr.prefix == null && "style".equals(attr.name)) {
                styleIndex = (short)(i + 1);
            } else if (attr.prefix == null && "class".equals(attr.name)) {
                classIndex = (short)(i + 1);
            }
        }

        header.size=36+20*attrs.size();
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
        w.write(stringIndex(null,namespace));
        w.write(stringIndex(null,name));
        w.write(attrStart);
        w.write(attrSize);
        w.write((short)attrs.size());
        w.write(idIndex);
        w.write(classIndex);
        w.write(styleIndex);
        for (AttrChunk a:attrs){
            a.write(w);
        }
    }
}
