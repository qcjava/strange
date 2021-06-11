/*-
 * #%L
 * Strange
 * %%
 * Copyright (C) 2021 Johan Vos
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Johan Vos nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.redfx.strange.gate;

import org.redfx.strange.Block;
import org.redfx.strange.BlockGate;
import org.redfx.strange.ControlledBlockGate;
import org.redfx.strange.Step;
import java.util.HashMap;

/**
 *
 * @author johan
 */
public class AddIntegerModulus extends BlockGate<AddIntegerModulus> {

    Block block;

    
    /**
     * Add integer a to the qubit in the x register mod N, result is in x
     * @param x0 start idx x register
     * @param x1 end idx x register, this must be |0> 
     * ANC(0)--- ANC(0)
     * x1 is an overflow bit
     * the qubit following x_1 should be 0 (and will be 0 after this gate)
     */
    public AddIntegerModulus(int x0, int x1, int a, int N) {
        super();
        System.err.println("asked for addintegermodulus, x0 = "+x0+", x1 = "+x1+", a = "+a+", N = "+N);
        setIndex(x0);
        x1 = x1 - x0;
        x0 = 0;
        this.block = createBlock(x0, x1, a, N);
        setBlock(block);
    }
    
    public Block createBlock(int x0, int x1, int a, int N) {
        Block answer = new Block("AddIntegerModulus", x1-x0+2);
        int n = x1-x0;
        int dim = n+1;
        System.err.println("Create block for AIM, n = "+n+", dim = " + dim+", x0 = "+x0);
        System.err.println("step 1, addi, x0 = "+x0+", x1 = " +x1+", a = "+a);
        AddInteger add = new AddInteger(x0, x1, a);
        answer.addStep(new Step(add));
answer.addStep(new Step(Step.Type.PSEUDO));
        AddInteger min = new AddInteger(x0,x1,N).inverse();
        System.err.println("step 2, addmin, x0 = "+x0+", x1 = " + x1+", N = " +N);
        answer.addStep(new Step(min));
        answer.addStep(new Step(Step.Type.PSEUDO));

        answer.addStep(new Step(new Cnot(x1,dim)));
        answer.addStep(new Step(Step.Type.PSEUDO));

        AddInteger addN = new AddInteger(x0,x1,N);
        ControlledBlockGate cbg = new ControlledBlockGate(addN, x0,dim);
        answer.addStep(new Step(cbg));

        AddInteger add2 = new AddInteger(x0,x1,a).inverse();
        answer.addStep(new Step(add2));
        
        answer.addStep(new Step(new X(dim-1)));
        answer.addStep(new Step(new Cnot(x1,dim)));
        answer.addStep(new Step(new X(dim-1)));

        AddInteger add3 = new AddInteger(x0,x1,a);
        answer.addStep (new Step(add3));
        System.err.println("AIM block created with some steps");
        return answer;
    }

}
