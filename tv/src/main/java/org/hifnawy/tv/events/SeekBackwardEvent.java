/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package org.hifnawy.tv.events;

public class SeekBackwardEvent {
    public static final int MINIMUM_SEEK_SPEED = 2000;
    private int seek = MINIMUM_SEEK_SPEED;

    public SeekBackwardEvent() {
        setSeek(MINIMUM_SEEK_SPEED);
    }

    public void setSeek(int seek) {
        if (seek < 0) throw new IllegalArgumentException("Seek speed must be larger than 0");
        this.seek = seek;
    }

    public int getSeek() {
        return seek > 0 ? -1 * seek : seek;
    }
}