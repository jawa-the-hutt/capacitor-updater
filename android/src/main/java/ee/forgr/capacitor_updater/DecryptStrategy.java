/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package ee.forgr.capacitor_updater;

import com.getcapacitor.JSObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.Objects;

public class DecryptStrategy {

  private String type = null;
  private String key = null;

  public DecryptStrategy(String type, String key) {
    this.type = type;
    this.key = key;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public JSObject toJSON() {
    final JSObject result = new JSObject();
    result.put("type", this.getType());
    result.put("key", this.getKey());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DecryptStrategy)) return false;
    DecryptStrategy that = (DecryptStrategy) o;
    return (
      getType() == that.getType() && Objects.equals(getKey(), that.getKey())
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getKey());
  }

  @Override
  public String toString() {
    return this.toJSON().toString();
  }
}
