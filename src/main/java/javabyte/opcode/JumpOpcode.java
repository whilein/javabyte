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

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum JumpOpcode {
    IFEQ(153),
    IFNE(154),
    IFLT(155),
    IFGE(156),
    IFGT(157),
    IFLE(158),
    IF_ICMPEQ(159),
    IF_ICMPNE(160),
    IF_ICMPLT(161),
    IF_ICMPGE(162),
    IF_ICMPGT(163),
    IF_ICMPLE(164),
    IF_ACMPEQ(165),
    IF_ACMPNE(166),
    GOTO(167);

    int opcode;
}