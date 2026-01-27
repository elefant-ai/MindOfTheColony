package com.goodbird.mindofthecolony.utils;

import java.util.ArrayList;

public class MessageBuffer {
    private final ArrayList<String> msgs;
    private final int maxSize;

    public MessageBuffer(int maxSize) {
        this.msgs = new ArrayList<>();
        this.maxSize = maxSize;
    }

    public void addMsg(String msg) {
        this.msgs.add(msg);
        if (this.msgs.size() > this.maxSize) {
            this.msgs.remove(0);
        }
    }

    private void dump() {
        this.msgs.clear();
    }

    public String dumpAndGetString() {
        if (msgs.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < msgs.size(); i++) {
            sb.append(String.format("\"%s\"", msgs.get(i).replace("\"", "\\\"")));
            if (i < msgs.size() - 1) {
                sb.append(",");
            }
        }
        dump();
        return sb.append("]").toString();
    }
}
