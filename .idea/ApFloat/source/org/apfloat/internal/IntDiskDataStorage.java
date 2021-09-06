/*
 * MIT License
 *
 * Copyright (c) 2002-2021 Mikko Tommila
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.apfloat.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Disk-based data storage for the <code>int</code> element type.
 *
 * @version 1.8.0
 * @author Mikko Tommila
 */

public class IntDiskDataStorage
    extends DiskDataStorage
{
    /**
     * Default constructor.
     */

    public IntDiskDataStorage()
        throws ApfloatRuntimeException
    {
    }

    /**
     * Subsequence constructor.
     *
     * @param intDiskDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected IntDiskDataStorage(IntDiskDataStorage intDiskDataStorage, long offset, long length)
    {
        super(intDiskDataStorage, offset, length);
    }

    @Override
    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new IntDiskDataStorage(this, offset + getOffset(), length);
    }

    private class IntDiskArrayAccess
        extends IntMemoryArrayAccess
    {
        // fileOffset is absolute position in file
        public IntDiskArrayAccess(int mode, long fileOffset, int length)
            throws ApfloatRuntimeException
        {
            super(new int[length], 0, length);
            this.mode = mode;
            this.fileOffset = fileOffset;

            if ((mode & READ) != 0)
            {
                int[] array = getIntData();
                WritableByteChannel out = new WritableByteChannel()
                {
                    @Override
                    public int write(ByteBuffer buffer)
                    {
                        IntBuffer src = buffer.asIntBuffer();
                        int readLength = src.remaining();

                        src.get(array, this.readPosition, readLength);

                        this.readPosition += readLength;
                        buffer.position(buffer.position() + readLength * 4);

                        return readLength * 4;
                    }

                    @Override public void close() {}
                    @Override public boolean isOpen() { return true; }

                    private int readPosition = 0;
                };

                transferTo(out, fileOffset * 4, (long) length * 4);
            }
        }

        @Override
        public void close()
            throws ApfloatRuntimeException
        {
            if ((this.mode & WRITE) != 0 && getData() != null)
            {
                int[] array = getIntData();
                ReadableByteChannel in = new ReadableByteChannel()
                {
                    @Override
                    public int read(ByteBuffer buffer)
                    {
                        IntBuffer dst = buffer.asIntBuffer();
                        int writeLength = dst.remaining();

                        dst.put(array, this.writePosition, writeLength);

                        this.writePosition += writeLength;
                        buffer.position(buffer.position() + writeLength * 4);

                        return writeLength * 4;
                    }

                    @Override public void close() {}
                    @Override public boolean isOpen() { return true; }

                    private int writePosition = 0;
                };

                transferFrom(in, this.fileOffset * 4, (long) array.length * 4);
            }

            super.close();
        }

        private static final long serialVersionUID = -88509093904437138L;

        private int mode;
        private long fileOffset;
    }

    @Override
    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new IntDiskArrayAccess(mode, getOffset() + offset, length);
    }

    @Override
    protected ArrayAccess createArrayAccess(int mode, int startColumn, int columns, int rows)
    {
        return new MemoryArrayAccess(mode, new int[columns * rows], startColumn, columns, rows);
    }

    @Override
    protected ArrayAccess createTransposedArrayAccess(int mode, int startColumn, int columns, int rows)
    {
        return new TransposedMemoryArrayAccess(mode, new int[columns * rows], startColumn, columns, rows);
    }

    private class MemoryArrayAccess
        extends IntMemoryArrayAccess
    {
        public MemoryArrayAccess(int mode, int[] data, int startColumn, int columns, int rows)
        {
            super(data, 0, data.length);
            this.mode = mode;
            this.startColumn = startColumn;
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public void close()
            throws ApfloatRuntimeException
        {
            if ((this.mode & WRITE) != 0 && getData() != null)
            {
                setArray(this, this.startColumn, this.columns, this.rows);
            }
            super.close();
        }

        private static final long serialVersionUID = 7690849230285450035L;

        private int mode,
                    startColumn,
                    columns,
                    rows;
    }

    private class TransposedMemoryArrayAccess
        extends IntMemoryArrayAccess
    {
        public TransposedMemoryArrayAccess(int mode, int[] data, int startColumn, int columns, int rows)
        {
            super(data, 0, data.length);
            this.mode = mode;
            this.startColumn = startColumn;
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public void close()
            throws ApfloatRuntimeException
        {
            if ((this.mode & WRITE) != 0 && getData() != null)
            {
                setTransposedArray(this, this.startColumn, this.columns, this.rows);
            }
            super.close();
        }

        private static final long serialVersionUID = 2990517367865486151L;

        private int mode,
                    startColumn,
                    columns,
                    rows;
    }

    private class BlockIterator
        extends AbstractIterator
    {
        public BlockIterator(int mode, long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            super(mode, startPosition, endPosition);
            this.arrayAccess = null;
            this.remaining = 0;
        }

        @Override
        public void next()
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkLength();

            assert (this.remaining > 0);

            checkAvailable();

            this.offset += getIncrement();
            this.remaining--;

            if (this.remaining == 0)
            {
                close();
            }

            super.next();
        }

        @Override
        public int getInt()
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkGet();
            checkAvailable();
            return this.data[this.offset];
        }

        @Override
        public void setInt(int value)
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkSet();
            checkAvailable();
            this.data[this.offset] = value;
        }

        @Override
        public <T> T get(Class<T> type)
            throws UnsupportedOperationException, IllegalStateException
        {
            if (!(type.equals(Integer.TYPE)))
            {
                throw new UnsupportedOperationException("Unsupported data type " + type.getCanonicalName() + ", the only supported type is int");
            }
            @SuppressWarnings("unchecked")
            T value = (T) (Integer) getInt();
            return value;
        }

        @Override
        public <T> void set(Class<T> type, T value)
            throws UnsupportedOperationException, IllegalArgumentException, IllegalStateException
        {
            if (!(type.equals(Integer.TYPE)))
            {
                throw new UnsupportedOperationException("Unsupported data type " + type.getCanonicalName() + ", the only supported type is int");
            }
            if (!(value instanceof Integer))
            {
                throw new IllegalArgumentException("Unsupported value type " + value.getClass().getCanonicalName() + ", the only supported type is Integer");
            }
            setInt((Integer) value);
        }

        /**
         * Closes the iterator. This needs to be called only if the
         * iterator is not iterated to the end.
         */

        @Override
        public void close()
            throws ApfloatRuntimeException
        {
            if (this.arrayAccess != null)
            {
                this.data = null;
                this.arrayAccess.close();
                this.arrayAccess = null;
            }
        }

        private void checkAvailable()
            throws ApfloatRuntimeException
        {
            if (this.arrayAccess == null)
            {
                boolean isForward = (getIncrement() > 0);
                int length = (int) Math.min(getLength(), getBlockSize() / 4);
                long offset = (isForward ? getPosition() : getPosition() - length + 1);

                this.arrayAccess = getArray(getMode(), offset, length);
                this.data = this.arrayAccess.getIntData();
                this.offset = this.arrayAccess.getOffset() + (isForward ? 0 : length - 1);
                this.remaining = length;
            }
        }

        private static final long serialVersionUID = 4187202582650284101L;

        private ArrayAccess arrayAccess;
        private int[] data;
        private int offset,
                    remaining;
    }

    @Override
    public Iterator iterator(int mode, long startPosition, long endPosition)
        throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
    {
        if ((mode & READ_WRITE) == 0)
        {
            throw new IllegalArgumentException("Illegal mode: " + mode);
        }
        return new BlockIterator(mode, startPosition, endPosition);
    }

    @Override
    protected int getUnitSize()
    {
        return 4;
    }

    private static final long serialVersionUID = -1540087135754114721L;
}
