package com.nextlabs.nxl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

class SectionHeader {

    private int sectionMap;
    private Section[] sections;

    public SectionHeader() {
        sections = new Section[32];
    }

    public SectionHeader(ByteBuffer bb) {
        sections = new Section[32];
        sectionMap = bb.getInt();
        for (int i = 0; i < 32; ++i) {
            if (NxlUtils.isFlagSet(sectionMap, 1 << i)) {
                sections[i] = new Section(bb);
            } else {
                bb.position(bb.position() + 64);
            }
        }
    }

    public void writeTo(ByteBuffer bb) {
        bb.putInt(sectionMap);
        for (Section section : sections) {
            if (section == null) {
                bb.position(bb.position() + 64);
            } else {
                section.writeTo(bb);
            }
        }
    }

    public void writeSectionData(OutputStream os) throws IOException {
        byte[] emptyBuf = new byte[4096];
        for (Section section : sections) {
            if (section != null) {
                byte[] data = section.getData();
                os.write(data);
                os.write(emptyBuf, 0, 4096 - data.length);
            }
        }
    }

    public void writeSectionData(RandomAccessFile raf) throws IOException {
        byte[] emptyBuf = new byte[4096];
        for (Section section : sections) {
            if (section != null) {
                byte[] data = section.getData();
                raf.write(data);
                raf.write(emptyBuf, 0, 4096 - data.length);
            }
        }
    }

    public void setSection(int index, Section section, int contentOffset) {
        sectionMap |= 1 << index;
        section.setStartOffset(contentOffset);
        sections[index] = section;
    }

    public Section getSection(int index) {
        return sections[index];
    }

    public Section getSectionByName(String name) {
        for (Section section : sections) {
            if (section != null && name.equals(section.getName())) {
                return section;
            }
        }
        return null;
    }
}
