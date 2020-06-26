/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2018, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.strange.gate;

import com.gluonhq.strange.Block;
import com.gluonhq.strange.BlockGate;
import com.gluonhq.strange.Step;
import static com.gluonhq.strange.gate.Add.cache;
import java.util.HashMap;

/**
 *
 * @author johan
 */
public class AddInteger extends BlockGate<AddInteger> {

    Block block;
    static HashMap<Integer, Block> cache = new HashMap<>();

    
    /**
     * Add the qubit in the x register and the y register, result is in x
     * @param x0 start idx x register
     * @param x1 end idx x register
     * @param num the integer to be added, (y_m.. y_0)
     * x_0 ----- y_0 + x_0
     * x_1 ----- y+1 + x_1
     */
    public AddInteger(int x0, int x1, int num) {
        super();
          int hash = 1000000 * x0 + 10000*x1+ num;
        System.err.println("ADDnumhash for "+x0+", " + x1+", "+num+" = "+hash);
        this.block = cache.get(hash);
        if (this.block == null) {
            System.err.println("not cached");
            this.block = createBlock(x0, x1, num);
            cache.put(hash, block);
        } else {
            System.err.println("ADDnum block cached!");
        }
        
        setBlock(block);
       
    }
    
    public Block createBlock(int x0, int x1, int num) {
        int m = x1-x0+1;
        Block answer = new Block("AddInteger ", m);
        answer.addStep(new Step(new Fourier(m, 0)));
        for (int i = 0; i < m; i++) {
           for (int j = 0; j < m-i ; j++) {
                int cr0 = m-j-i-1; System.err.println("addnum, createblock num = "+num+", cr = "+cr0);
                if ((num >> cr0 & 1 ) == 1) {
                    System.err.println("YES add step");
                    Step s = new Step(new R(2,1+j,i));
                    answer.addStep(s);
                }
            }
        }
        answer.addStep(new Step(new InvFourier(m, 0)));
        return answer;
    }
    
    

    
}