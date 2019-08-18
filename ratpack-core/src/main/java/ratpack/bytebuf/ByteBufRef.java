/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCounted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

/**
 * A byte buf that is backed by another, but has its own ref count lifecycle.
 */
public class ByteBufRef extends ByteBuf {

  private final ReferenceCounted counted;

  private final ByteBuf delegate;

  private class ReferenceCountingWrapper extends AbstractReferenceCounted {
    @Override
    protected void deallocate() {
      delegate.release();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
      return this;
    }
  }

  public ByteBufRef(ByteBuf delegate) {
    this.delegate = delegate;
    this.counted = new ReferenceCountingWrapper();
  }

  private ByteBufRef(ReferenceCounted counted, ByteBuf delegate) {
    this.counted = counted;
    this.delegate = delegate;
  }

  private ByteBufRef wrap(ByteBuf delegate) {
    return new ByteBufRef(counted, delegate);
  }

  @Override
  public ByteBuf retain(int increment) {
    touch();
    counted.retain(increment);
    return this;
  }

  @Override
  public ByteBuf retain() {
    touch();
    counted.retain();
    return this;
  }

  @Override
  public boolean release() {
    touch();
    return counted.release();
  }

  @Override
  public boolean release(int decrement) {
    touch();
    return counted.release(decrement);
  }

  @Override
  public int refCnt() {
    return counted.refCnt();
  }

  @Override
  public int capacity() {
    return delegate.capacity();
  }

  @Override
  public ByteBuf capacity(int newCapacity) {
    delegate.capacity(newCapacity);
    return this;
  }

  @Override
  public int maxCapacity() {
    return delegate.maxCapacity();
  }

  @Override
  public ByteBufAllocator alloc() {
    return delegate.alloc();
  }

  @Override
  @Deprecated
  public ByteOrder order() {
    return delegate.order();
  }

  @Override
  @Deprecated
  public ByteBuf order(ByteOrder endianness) {
    delegate.order(endianness);
    return this;
  }

  @Override
  public ByteBuf unwrap() {
    return delegate;
  }

  @Override
  public boolean isDirect() {
    return delegate.isDirect();
  }

  @Override
  public boolean isReadOnly() {
    return delegate.isReadOnly();
  }

  @Override
  public ByteBuf asReadOnly() {
    if (delegate.isReadOnly()) {
      return this;
    }

    return wrap(delegate.asReadOnly());
  }

  @Override
  public int readerIndex() {
    return delegate.readerIndex();
  }

  @Override
  public ByteBuf readerIndex(int readerIndex) {
    delegate.readerIndex(readerIndex);
    return this;
  }

  @Override
  public int writerIndex() {
    return delegate.writerIndex();
  }

  @Override
  public ByteBuf writerIndex(int writerIndex) {
    delegate.writerIndex(writerIndex);
    return this;
  }

  @Override
  public ByteBuf setIndex(int readerIndex, int writerIndex) {
    delegate.setIndex(readerIndex, writerIndex);
    return this;
  }

  @Override
  public int readableBytes() {
    return delegate.readableBytes();
  }

  @Override
  public int writableBytes() {
    return delegate.writableBytes();
  }

  @Override
  public int maxWritableBytes() {
    return delegate.maxWritableBytes();
  }

  @Override
  public boolean isReadable() {
    return delegate.isReadable();
  }

  @Override
  public boolean isReadable(int size) {
    return delegate.isReadable(size);
  }

  @Override
  public boolean isWritable() {
    return delegate.isWritable();
  }

  @Override
  public boolean isWritable(int size) {
    return delegate.isWritable(size);
  }

  @Override
  public ByteBuf clear() {
    delegate.clear();
    return this;
  }

  @Override
  public ByteBuf markReaderIndex() {
    delegate.markReaderIndex();
    return this;
  }

  @Override
  public ByteBuf resetReaderIndex() {
    delegate.resetReaderIndex();
    return this;
  }

  @Override
  public ByteBuf markWriterIndex() {
    delegate.markWriterIndex();
    return this;
  }

  @Override
  public ByteBuf resetWriterIndex() {
    delegate.resetWriterIndex();
    return this;
  }

  @Override
  public ByteBuf discardReadBytes() {
    delegate.discardReadBytes();
    return this;
  }

  @Override
  public ByteBuf discardSomeReadBytes() {
    delegate.discardSomeReadBytes();
    return this;
  }

  @Override
  public ByteBuf ensureWritable(int minWritableBytes) {
    delegate.ensureWritable(minWritableBytes);
    return this;
  }

  @Override
  public int ensureWritable(int minWritableBytes, boolean force) {
    return delegate.ensureWritable(minWritableBytes, force);
  }

  @Override
  public boolean getBoolean(int index) {
    return delegate.getBoolean(index);
  }

  @Override
  public byte getByte(int index) {
    return delegate.getByte(index);
  }

  @Override
  public short getUnsignedByte(int index) {
    return delegate.getUnsignedByte(index);
  }

  @Override
  public short getShort(int index) {
    return delegate.getShort(index);
  }

  @Override
  public short getShortLE(int index) {
    return delegate.getShortLE(index);
  }

  @Override
  public int getUnsignedShort(int index) {
    return delegate.getUnsignedShort(index);
  }

  @Override
  public int getUnsignedShortLE(int index) {
    return delegate.getUnsignedShortLE(index);
  }

  @Override
  public int getMedium(int index) {
    return delegate.getMedium(index);
  }

  @Override
  public int getMediumLE(int index) {
    return delegate.getMediumLE(index);
  }

  @Override
  public int getUnsignedMedium(int index) {
    return delegate.getUnsignedMedium(index);
  }

  @Override
  public int getUnsignedMediumLE(int index) {
    return delegate.getUnsignedMediumLE(index);
  }

  @Override
  public int getInt(int index) {
    return delegate.getInt(index);
  }

  @Override
  public int getIntLE(int index) {
    return delegate.getIntLE(index);
  }

  @Override
  public long getUnsignedInt(int index) {
    return delegate.getUnsignedInt(index);
  }

  @Override
  public long getUnsignedIntLE(int index) {
    return delegate.getUnsignedIntLE(index);
  }

  @Override
  public long getLong(int index) {
    return delegate.getLong(index);
  }

  @Override
  public long getLongLE(int index) {
    return delegate.getLongLE(index);
  }

  @Override
  public char getChar(int index) {
    return delegate.getChar(index);
  }

  @Override
  public float getFloat(int index) {
    return delegate.getFloat(index);
  }

  @Override
  public float getFloatLE(int index) {
    return delegate.getFloatLE(index);
  }

  @Override
  public double getDouble(int index) {
    return delegate.getDouble(index);
  }

  @Override
  public double getDoubleLE(int index) {
    return delegate.getDoubleLE(index);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst) {
    delegate.getBytes(index, dst);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int length) {
    delegate.getBytes(index, dst, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    delegate.getBytes(index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst) {
    delegate.getBytes(index, dst);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    delegate.getBytes(index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    delegate.getBytes(index, dst);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
    delegate.getBytes(index, out, length);
    return this;
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
    return delegate.getBytes(index, out, length);
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
    return delegate.getBytes(index, out, position, length);
  }

  @Override
  public CharSequence getCharSequence(int index, int length, Charset charset) {
    return delegate.getCharSequence(index, length, charset);
  }

  @Override
  public ByteBuf setBoolean(int index, boolean value) {
    delegate.setBoolean(index, value);
    return this;
  }

  @Override
  public ByteBuf setByte(int index, int value) {
    delegate.setByte(index, value);
    return this;
  }

  @Override
  public ByteBuf setShort(int index, int value) {
    delegate.setShort(index, value);
    return this;
  }

  @Override
  public ByteBuf setShortLE(int index, int value) {
    delegate.setShortLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setMedium(int index, int value) {
    delegate.setMedium(index, value);
    return this;
  }

  @Override
  public ByteBuf setMediumLE(int index, int value) {
    delegate.setMediumLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setInt(int index, int value) {
    delegate.setInt(index, value);
    return this;
  }

  @Override
  public ByteBuf setIntLE(int index, int value) {
    delegate.setIntLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setLong(int index, long value) {
    delegate.setLong(index, value);
    return this;
  }

  @Override
  public ByteBuf setLongLE(int index, long value) {
    delegate.setLongLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setChar(int index, int value) {
    delegate.setChar(index, value);
    return this;
  }

  @Override
  public ByteBuf setFloat(int index, float value) {
    delegate.setFloat(index, value);
    return this;
  }

  @Override
  public ByteBuf setFloatLE(int index, float value) {
    delegate.setFloatLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setDouble(int index, double value) {
    delegate.setDouble(index, value);
    return this;
  }

  @Override
  public ByteBuf setDoubleLE(int index, double value) {
    delegate.setDoubleLE(index, value);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src) {
    delegate.setBytes(index, src);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int length) {
    delegate.setBytes(index, src, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    delegate.setBytes(index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src) {
    delegate.setBytes(index, src);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    delegate.setBytes(index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    delegate.setBytes(index, src);
    return this;
  }

  @Override
  public int setBytes(int index, InputStream in, int length) throws IOException {
    return delegate.setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    return delegate.setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
    return delegate.setBytes(index, in, position, length);
  }

  @Override
  public ByteBuf setZero(int index, int length) {
    delegate.setZero(index, length);
    return this;
  }

  @Override
  public int setCharSequence(int index, CharSequence sequence, Charset charset) {
    return delegate.setCharSequence(index, sequence, charset);
  }

  @Override
  public boolean readBoolean() {
    return delegate.readBoolean();
  }

  @Override
  public byte readByte() {
    return delegate.readByte();
  }

  @Override
  public short readUnsignedByte() {
    return delegate.readUnsignedByte();
  }

  @Override
  public short readShort() {
    return delegate.readShort();
  }

  @Override
  public short readShortLE() {
    return delegate.readShortLE();
  }

  @Override
  public int readUnsignedShort() {
    return delegate.readUnsignedShort();
  }

  @Override
  public int readUnsignedShortLE() {
    return delegate.readUnsignedShortLE();
  }

  @Override
  public int readMedium() {
    return delegate.readMedium();
  }

  @Override
  public int readMediumLE() {
    return delegate.readMediumLE();
  }

  @Override
  public int readUnsignedMedium() {
    return delegate.readUnsignedMedium();
  }

  @Override
  public int readUnsignedMediumLE() {
    return delegate.readUnsignedMediumLE();
  }

  @Override
  public int readInt() {
    return delegate.readInt();
  }

  @Override
  public int readIntLE() {
    return delegate.readIntLE();
  }

  @Override
  public long readUnsignedInt() {
    return delegate.readUnsignedInt();
  }

  @Override
  public long readUnsignedIntLE() {
    return delegate.readUnsignedIntLE();
  }

  @Override
  public long readLong() {
    return delegate.readLong();
  }

  @Override
  public long readLongLE() {
    return delegate.readLongLE();
  }

  @Override
  public char readChar() {
    return delegate.readChar();
  }

  @Override
  public float readFloat() {
    return delegate.readFloat();
  }

  @Override
  public float readFloatLE() {
    return delegate.readFloatLE();
  }

  @Override
  public double readDouble() {
    return delegate.readDouble();
  }

  @Override
  public double readDoubleLE() {
    return delegate.readDoubleLE();
  }

  @Override
  public ByteBuf readBytes(int length) {
    return delegate.readBytes(length);
  }

  @Override
  public ByteBuf readSlice(int length) {
    return delegate.readSlice(length);
  }

  @Override
  public ByteBuf readRetainedSlice(int length) {
    return delegate.readRetainedSlice(length);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst) {
    delegate.readBytes(dst);
    return this;
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int length) {
    delegate.readBytes(dst, length);
    return this;
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
    delegate.readBytes(dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf readBytes(byte[] dst) {
    delegate.readBytes(dst);
    return this;
  }

  @Override
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
    delegate.readBytes(dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf readBytes(ByteBuffer dst) {
    delegate.readBytes(dst);
    return this;
  }

  @Override
  public ByteBuf readBytes(OutputStream out, int length) throws IOException {
    delegate.readBytes(out, length);
    return this;
  }

  @Override
  public int readBytes(GatheringByteChannel out, int length) throws IOException {
    return delegate.readBytes(out, length);
  }

  @Override
  public CharSequence readCharSequence(int length, Charset charset) {
    return delegate.readCharSequence(length, charset);
  }

  @Override
  public int readBytes(FileChannel out, long position, int length) throws IOException {
    return delegate.readBytes(out, position, length);
  }

  @Override
  public ByteBuf skipBytes(int length) {
    delegate.skipBytes(length);
    return this;
  }

  @Override
  public ByteBuf writeBoolean(boolean value) {
    delegate.writeBoolean(value);
    return this;
  }

  @Override
  public ByteBuf writeByte(int value) {
    delegate.writeByte(value);
    return this;
  }

  @Override
  public ByteBuf writeShort(int value) {
    delegate.writeShort(value);
    return this;
  }

  @Override
  public ByteBuf writeShortLE(int value) {
    delegate.writeShortLE(value);
    return this;
  }

  @Override
  public ByteBuf writeMedium(int value) {
    delegate.writeMedium(value);
    return this;
  }

  @Override
  public ByteBuf writeMediumLE(int value) {
    delegate.writeMediumLE(value);
    return this;
  }

  @Override
  public ByteBuf writeInt(int value) {
    delegate.writeInt(value);
    return this;
  }

  @Override
  public ByteBuf writeIntLE(int value) {
    delegate.writeIntLE(value);
    return this;
  }

  @Override
  public ByteBuf writeLong(long value) {
    delegate.writeLong(value);
    return this;
  }

  @Override
  public ByteBuf writeLongLE(long value) {
    delegate.writeLongLE(value);
    return this;
  }

  @Override
  public ByteBuf writeChar(int value) {
    delegate.writeChar(value);
    return this;
  }

  @Override
  public ByteBuf writeFloat(float value) {
    delegate.writeFloat(value);
    return this;
  }

  @Override
  public ByteBuf writeFloatLE(float value) {
    delegate.writeFloatLE(value);
    return this;
  }

  @Override
  public ByteBuf writeDouble(double value) {
    delegate.writeDouble(value);
    return this;
  }

  @Override
  public ByteBuf writeDoubleLE(double value) {
    delegate.writeDoubleLE(value);
    return this;
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src) {
    delegate.writeBytes(src);
    return this;
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int length) {
    delegate.writeBytes(src, length);
    return this;
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
    delegate.writeBytes(src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf writeBytes(byte[] src) {
    delegate.writeBytes(src);
    return this;
  }

  @Override
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
    delegate.writeBytes(src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf writeBytes(ByteBuffer src) {
    delegate.writeBytes(src);
    return this;
  }

  @Override
  public int writeBytes(InputStream in, int length) throws IOException {
    return delegate.writeBytes(in, length);
  }

  @Override
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
    return delegate.writeBytes(in, length);
  }

  @Override
  public int writeBytes(FileChannel in, long position, int length) throws IOException {
    return delegate.writeBytes(in, position, length);
  }

  @Override
  public ByteBuf writeZero(int length) {
    delegate.writeZero(length);
    return this;
  }

  @Override
  public int writeCharSequence(CharSequence sequence, Charset charset) {
    return delegate.writeCharSequence(sequence, charset);
  }

  @Override
  public int indexOf(int fromIndex, int toIndex, byte value) {
    return delegate.indexOf(fromIndex, toIndex, value);
  }

  @Override
  public int bytesBefore(byte value) {
    return delegate.bytesBefore(value);
  }

  @Override
  public int bytesBefore(int length, byte value) {
    return delegate.bytesBefore(length, value);
  }

  @Override
  public int bytesBefore(int index, int length, byte value) {
    return delegate.bytesBefore(index, length, value);
  }

  @Override
  public int forEachByte(ByteProcessor processor) {
    return delegate.forEachByte(processor);
  }

  @Override
  public int forEachByte(int index, int length, ByteProcessor processor) {
    return delegate.forEachByte(index, length, processor);
  }

  @Override
  public int forEachByteDesc(ByteProcessor processor) {
    return delegate.forEachByteDesc(processor);
  }

  @Override
  public int forEachByteDesc(int index, int length, ByteProcessor processor) {
    return delegate.forEachByteDesc(index, length, processor);
  }

  @Override
  public ByteBuf copy() {
    return delegate.copy();
  }

  @Override
  public ByteBuf copy(int index, int length) {
    return delegate.copy(index, length);
  }

  @Override
  public ByteBuf slice() {
    return wrap(delegate.slice());
  }

  @Override
  public ByteBuf retainedSlice() {
    return wrap(delegate.retainedSlice());
  }

  @Override
  public ByteBuf slice(int index, int length) {
    return wrap(delegate.slice(index, length));
  }

  @Override
  public ByteBuf retainedSlice(int index, int length) {
    return wrap(delegate.retainedSlice(index, length));
  }

  @Override
  public ByteBuf duplicate() {
    return wrap(delegate.duplicate());
  }

  @Override
  public ByteBuf retainedDuplicate() {
    return wrap(delegate.retainedDuplicate());
  }

  @Override
  public int nioBufferCount() {
    return delegate.nioBufferCount();
  }

  @Override
  public ByteBuffer nioBuffer() {
    return delegate.nioBuffer();
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    return delegate.nioBuffer(index, length);
  }

  @Override
  public ByteBuffer internalNioBuffer(int index, int length) {
    return delegate.internalNioBuffer(index, length);
  }

  @Override
  public ByteBuffer[] nioBuffers() {
    return delegate.nioBuffers();
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    return delegate.nioBuffers(index, length);
  }

  @Override
  public boolean hasArray() {
    return delegate.hasArray();
  }

  @Override
  public byte[] array() {
    return delegate.array();
  }

  @Override
  public int arrayOffset() {
    return delegate.arrayOffset();
  }

  @Override
  public boolean hasMemoryAddress() {
    return delegate.hasMemoryAddress();
  }

  @Override
  public long memoryAddress() {
    return delegate.memoryAddress();
  }

  @Override
  public String toString(Charset charset) {
    return delegate.toString(charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    return delegate.toString(index, length, charset);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public int compareTo(ByteBuf buffer) {
    return delegate.compareTo(buffer);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public ByteBuf touch() {
    counted.touch();
    delegate.touch();
    return this;
  }

  @Override
  public ByteBuf touch(Object hint) {
    counted.touch();
    delegate.touch(hint);
    return this;
  }
}
