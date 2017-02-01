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

import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.UnsignedBytes;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import se.sics.kompics.network.Address;

public class NetAddress implements Serializable, Address, Comparable<NetAddress> {

    private final static long serialVersionUID = 2536770490757392511L;
    private final InetSocketAddress isa;

    public NetAddress(InetAddress addr, int portI) {
        this.isa = new InetSocketAddress(addr, portI);
    }

    @Override
    public InetAddress getIp() {
        return this.isa.getAddress();
    }

    @Override
    public int getPort() {
        return this.isa.getPort();
    }

    @Override
    public InetSocketAddress asSocket() {
        return this.isa;
    }

    @Override
    public boolean sameHostAs(Address other) {
        return this.isa.equals(other.asSocket());
    }

    @Override
    public final String toString() {
        return isa.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = ((11 * hash) + ((this.isa != null) ? this.isa.hashCode() : 0));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetAddress)) {
            return false;
        }
        final NetAddress that = ((NetAddress) obj);
        return this.compareTo(that) == 0;
    }

    @Override
    public int compareTo(NetAddress that) {
        return ComparisonChain.start()
                .compare(this.isa.getAddress().getAddress(), that.isa.getAddress().getAddress(), UnsignedBytes.lexicographicalComparator())
                .compare(this.isa.getPort(), that.isa.getPort())
                .result();
    }
}
