/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import se.sics.kompics.config.Conversions;
import se.sics.kompics.config.Converter;

public class NetAddressConverter implements Converter<NetAddress> {

    @Override
    public NetAddress convert(Object o) {
        if (o instanceof Map) {
            try {
                Map m = ((Map) o);
                String hostname = Conversions.convert(m.get("ip"), String.class);
                int p = Conversions.convert(m.get("port"), Integer.class);
                InetAddress ip = InetAddress.getByName(hostname);
                return new NetAddress(ip, p);
            } catch (UnknownHostException ex) {
                return null;
            }
        }
        if (o instanceof String) {
            try {
                String[] ipport = ((String) o).split(":");
                InetAddress ip = InetAddress.getByName(ipport[0]);
                int p = Integer.parseInt(ipport[1]);
                return new NetAddress(ip, p);
            } catch (UnknownHostException ex) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Class<NetAddress> type() {
        return NetAddress.class;
    }
}
