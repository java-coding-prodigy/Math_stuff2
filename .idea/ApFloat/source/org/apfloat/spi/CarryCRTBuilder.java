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
package org.apfloat.spi;

/**
 * Interface of a factory for creating carry-CRT related objects.
 * The factory method pattern is used.
 *
 * @param <T> The element array type of the CRT.
 *
 * @see CarryCRTStrategy
 * @see CarryCRTStepStrategy
 *
 * @since 1.7.0
 * @version 1.7.0
 * @author Mikko Tommila
 */

public interface CarryCRTBuilder<T>
{
    /**
     * Creates an object for implementing the carry-CRT of a three-NTT
     * based convolution using the specified radix.
     *
     * @param radix The radix that will be used.
     *
     * @return A suitable object for performing the carry-CRT.
     */

    public CarryCRTStrategy createCarryCRT(int radix);

    /**
     * Creates an object for implementing the steps of the carry-CRT
     * of a three-NTT based convolution using the specified radix.
     *
     * @param radix The radix that will be used.
     *
     * @return A suitable object for performing the carry-CRT steps.
     */

    public CarryCRTStepStrategy<T> createCarryCRTSteps(int radix);
}
