/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.data.model.local;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * Model class for the ExpandableListView footer.
 * @author shalzz
 *
 */
@AutoValue
public abstract class ListFooter implements Parcelable {

	public abstract Float getHeld();
	public abstract Float getAttended();

    private Float percentage;

	public Float getPercentage() {
        if(percentage == null) {
            percentage = getHeld() > 0f ? getAttended() / getHeld() * 100 : 0.0f;
        }
        return percentage;
	}

    public static Builder builder() {
        return new AutoValue_ListFooter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setHeld(Float value);
        public abstract Builder setAttended(Float value);
        public abstract ListFooter build();
    }
}
