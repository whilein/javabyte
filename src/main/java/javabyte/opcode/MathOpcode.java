/*
 *    Copyright 2021 Whilein
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package javabyte.opcode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author whilein
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum MathOpcode {
    IADD(96), ISUB(100), IMUL(104), IDIV(108), IREM(112), INEG(116),
    LADD(97), LSUB(101), LMUL(105), LDIV(109), LREM(113), LNEG(117),
    FADD(98), FSUB(102), FMUL(106), FDIV(110), FREM(114), FNEG(118),
    DADD(99), DSUB(103), DMUL(107), DDIV(111), DREM(115), DNEG(119),
    ISHL(120), LSHL(121), ISHR(122), LSHR(123), IUSHR(124), LUSHR(125),
    IAND(126), LAND(127), IOR(128), LOR(129), IXOR(130), LXOR(131);

    int opcode;

}