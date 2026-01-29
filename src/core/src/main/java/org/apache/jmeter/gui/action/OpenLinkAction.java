/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.gui.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenLinkAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(OpenLinkAction.class);

    private static final Map<String, String> LINK_MAP =
            initLinkMap();

    private static final Set<String> commands = new HashSet<>();

    static {
        commands.add(ActionNames.LINK_BUG_TRACKER);
        commands.add(ActionNames.LINK_COMP_REF);
        commands.add(ActionNames.LINK_FUNC_REF);
        commands.add(ActionNames.LINK_NIGHTLY_BUILD);
        commands.add(ActionNames.LINK_RELEASE_NOTES);
    }

    private static final Map<String, String> initLinkMap() {
        Map<String, String> map = new HashMap<>(5);
        map.put(ActionNames.LINK_BUG_TRACKER, "https://www.psbank.ru/bank/af000010");
        map.put(ActionNames.LINK_COMP_REF, "https://www.psbank.ru/bank/af000010");
        map.put(ActionNames.LINK_FUNC_REF, "https://www.psbank.ru/bank/af000010");
        map.put(ActionNames.LINK_NIGHTLY_BUILD, "https://www.psbank.ru/bank/af000010");
        map.put(ActionNames.LINK_RELEASE_NOTES, "https://www.psbank.ru/bank/af000010");
        return map;
    }
    /**
     * @see org.apache.jmeter.gui.action.Command#doAction(ActionEvent)
     */
    @Override
    public void doAction(ActionEvent e) {
        String url = LINK_MAP.get(e.getActionCommand());
        if(url == null) {
            log.warn("Действие {}, не обрабатываемое этим классом", e.getActionCommand());
            return;
        }
        try {
            if(e.getSource() instanceof String[]) {
                url += "#"+((String[])e.getSource())[1];
            }
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException err) {
            log.error(
                    "OpenLinkAction: Браузер пользователя по умолчанию не найден или его не удается запустить,"
                    + " или не удалось запустить приложение-обработчик по умолчанию в {}",
                    url, err);
        } catch (UnsupportedOperationException err) {
            log.error("OpenLinkAction: Текущая платформа не поддерживает рабочий стол.Действие.ПРОСМОТРИТЕ действие на {}", url, err);
            showBrowserWarning(url);
        } catch (SecurityException err) {
            log.error("OpenLinkAction: проблема с безопасностью на {}", url, err);
        } catch (Exception err) {
            log.error("OpenLinkAction на {}", url, err);
        }
    }

    @Override
    public Set<String> getActionNames() {
        return commands;
    }

    private void showBrowserWarning(String url) {
        String problemSolver;
        if (url.startsWith(LINK_MAP.get(ActionNames.LINK_COMP_REF))
                || url.startsWith(LINK_MAP.get(ActionNames.LINK_FUNC_REF))) {
            problemSolver = "\n\nПопробуйте установить системному свойству help.local значение true.";
        } else {
            problemSolver = "";
        }
        JOptionPane.showMessageDialog(null, String.format(
                "Проблема с открытием браузера для отображения содержимого URL%n%s%s",
                url, problemSolver), "Проблема с открытием браузера",
                JOptionPane.WARNING_MESSAGE);
    }

}
